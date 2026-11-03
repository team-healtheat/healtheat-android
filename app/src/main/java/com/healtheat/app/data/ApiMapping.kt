package com.healtheat.app.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.time.LocalDate
import java.time.format.DateTimeParseException

// API contract includes required concepts but not full JSON examples. Keep adaptation in one file.
private fun JsonElement?.asObjectOrNull(): JsonObject? = if (this != null && isJsonObject) asJsonObject else null
private fun JsonElement?.asArrayOrNull(): JsonArray? = if (this != null && isJsonArray) asJsonArray else null
private fun JsonObject?.read(vararg names: String): JsonElement? = names.firstNotNullOfOrNull { key ->
    this?.get(key)?.takeUnless { it.isJsonNull }
}
private fun JsonObject?.text(vararg names: String): String? = read(*names)?.let { element ->
    if (element.isJsonPrimitive) element.asString else null
}
private fun JsonObject?.number(vararg names: String): Long? = text(*names)?.toLongOrNull()
private fun JsonObject?.flag(vararg names: String): Boolean? = text(*names)?.toBooleanStrictOrNull()
private fun JsonObject?.list(vararg names: String): List<JsonElement> = read(*names).asArrayOrNull()?.toList() ?: emptyList()
private fun JsonElement?.unwrap(): JsonElement? {
    if (this == null || isJsonNull) return null
    val wrapper = asObjectOrNull()
    return if (wrapper != null && wrapper.has("data")) wrapper.get("data")?.takeUnless { it.isJsonNull } else this
}
private fun JsonElement?.requiredObject(label: String) = unwrap().asObjectOrNull()
    ?: throw IllegalArgumentException("$label 응답 JSON 형식을 확인해 주세요 (API DTO와 앱 파서 불일치)")

/** A missing today-plan may be 204, 404, null data, or an explicit exists=false. */
fun JsonElement?.toPlanOrNull(): Pair<Long, Map<MealType, Meal>>? {
    val root = unwrap() ?: return null
    val plan = root.asObjectOrNull() ?: return null
    if (plan.flag("exists", "hasPlan") == false) return null
    val inner = plan.read("mealPlan")?.asObjectOrNull() ?: plan
    val planId = inner.number("id", "planId")
        ?: throw IllegalArgumentException("식단 ID가 없어요. MealPlanResponse.id를 확인해 주세요")
    val mealElement = inner.read("meals", "recommendations")
    val list: List<JsonElement> = mealElement.asArrayOrNull()?.toList()
        ?: mealElement.asObjectOrNull()?.entrySet()?.map { it.value } ?: emptyList()
    if (list.isEmpty()) throw IllegalArgumentException("식단의 meals[] 필드를 읽지 못했어요")
    val meals = mutableMapOf<MealType, Meal>()
    for (item in list) {
        val row = item.asObjectOrNull() ?: continue
        if (row.flag("isCurrent", "current") == false) continue
        val type = runCatching { MealType.valueOf(row.text("mealType", "type") ?: "") }.getOrNull() ?: continue
        val foods = row.list("items", "mealItems", "foods").mapNotNull { foodElement ->
            val food = foodElement.asObjectOrNull() ?: return@mapNotNull null
            val foodName = food.text("foodName", "name") ?: return@mapNotNull null
            val preparation = runCatching { PreparationType.valueOf(food.text("preparationType", "preparation") ?: "READY_TO_EAT") }
                .getOrDefault(PreparationType.READY_TO_EAT)
            FoodItem(
                id = food.number("id", "mealItemId") ?: 0L,
                name = foodName,
                serving = food.text("servingDescription", "serving") ?: "",
                preparation = preparation,
                recipeId = food.number("recipeId") ?: food.read("recipe").asObjectOrNull()?.number("id")
            )
        }
        val source = runCatching { SourceType.valueOf(row.text("sourceType", "source") ?: "HOME_COOKED") }
            .getOrDefault(SourceType.HOME_COOKED)
        val meal = Meal(
            id = row.number("id", "recommendationId")
                ?: throw IllegalArgumentException("${type.label} 추천 ID가 없어요"),
            type = type, source = source,
            headline = foods.firstOrNull()?.name ?: type.label,
            reason = row.text("recommendationReason", "reason") ?: "",
            items = foods,
            version = row.text("version")?.toIntOrNull() ?: 1
        )
        meals[type] = meal
    }
    if (MealType.entries.any { it !in meals }) {
        throw IllegalArgumentException("식단에 아침·점심·저녁 중 누락된 끼니가 있어요")
    }
    return planId to meals
}

fun JsonElement?.toSingleMealOrNull(): Meal? {
    val root = unwrap().asObjectOrNull() ?: return null
    val meal = root.read("meal", "recommendation", "updatedMeal")?.asObjectOrNull() ?: root
    // Can't reuse toPlanOrNull, which intentionally requires all three meals.
    val type = runCatching { MealType.valueOf(meal.text("mealType") ?: "") }.getOrNull() ?: return null
    val foods = meal.list("items", "mealItems").mapNotNull { e ->
        val f = e.asObjectOrNull() ?: return@mapNotNull null
        FoodItem(f.number("id") ?: 0, f.text("foodName") ?: return@mapNotNull null,
            f.text("servingDescription") ?: "",
            runCatching { PreparationType.valueOf(f.text("preparationType") ?: "READY_TO_EAT") }.getOrDefault(PreparationType.READY_TO_EAT),
            recipeId = f.number("recipeId") ?: f.read("recipe").asObjectOrNull()?.number("id"))
    }
    return Meal(meal.number("id", "recommendationId") ?: return null, type,
        runCatching { SourceType.valueOf(meal.text("sourceType") ?: "HOME_COOKED") }.getOrDefault(SourceType.HOME_COOKED),
        foods.firstOrNull()?.name ?: type.label, meal.text("recommendationReason") ?: "", foods,
        meal.text("version")?.toIntOrNull() ?: 1)
}

fun JsonElement?.toRecipe(): Recipe {
    val root = requiredObject("레시피")
    val recipe = root.read("recipe")?.asObjectOrNull() ?: root
    val ingredients = recipe.list("ingredients", "recipeIngredients").mapNotNull { item ->
        val ob = item.asObjectOrNull()
        ob?.text("ingredientName", "name")?.let { name ->
            listOfNotNull(name, ob.text("quantity"), ob.text("unit")).joinToString(" ")
        } ?: item.takeIf { it.isJsonPrimitive }?.asString
    }
    val steps = recipe.list("steps", "recipeSteps").mapNotNull { item ->
        item.asObjectOrNull()?.text("instruction", "description") ?: item.takeIf { it.isJsonPrimitive }?.asString
    }
    return Recipe(recipe.text("title", "name") ?: "조리 방법",
        recipe.text("cookingTimeMinutes", "minutes")?.toIntOrNull() ?: 0, ingredients, steps)
}

fun JsonElement?.toRecords(): List<MealRecord> {
    val raw = unwrap()
    val elements = raw.asArrayOrNull()?.toList()
        ?: raw.asObjectOrNull()?.list("records", "items", "content") ?: emptyList()
    return elements.mapNotNull { element ->
        val record = element.asObjectOrNull() ?: return@mapNotNull null
        val date = try { LocalDate.parse(record.text("mealDate", "date") ?: "") }
        catch (_: DateTimeParseException) { return@mapNotNull null }
        val type = runCatching { MealType.valueOf(record.text("mealType") ?: "") }.getOrNull() ?: return@mapNotNull null
        val foods = record.list("items", "mealRecordItems").mapNotNull { row ->
            row.asObjectOrNull()?.text("foodName", "name") ?: row.takeIf { it.isJsonPrimitive }?.asString
        }
        MealRecord(record.number("id") ?: 0L, date, type, foods,
            record.flag("fromRecommendation") ?: false, record.number("recommendationId"))
    }
}

fun JsonElement?.toHealthFactors(): List<HealthFactor> {
    val raw = unwrap()
    val items = raw.asArrayOrNull()?.toList() ?: raw.asObjectOrNull()?.list("factors", "healthFactors", "items") ?: emptyList()
    return items.mapNotNull { e ->
        val f = e.asObjectOrNull() ?: return@mapNotNull null
        val details = f.read("healthFactor")?.asObjectOrNull()
        HealthFactor(f.number("factorId", "healthFactorId", "id") ?: 0L,
            f.text("name", "factorName", "label") ?: details.text("name") ?: return@mapNotNull null,
            f.text("reportedStatus", "status") ?: "",
            f.flag("isActive", "active") ?: true,
            f.text("type") ?: details.text("type") ?: "GOAL",
            f.text("supported") ?: details.text("supported") ?: "UNSUPPORTED",
            f.text("originalText") ?: "")
    }
}

fun JsonElement?.toCandidateFactors(): List<HealthFactor> {
    val root = unwrap()
    val factors = root.asArrayOrNull()?.toList()
        ?: root.asObjectOrNull()?.list("factors", "candidates", "extractedFactors", "healthFactors") ?: emptyList()
    return factors.mapNotNull { element ->
        val f = element.asObjectOrNull() ?: return@mapNotNull null
        HealthFactor(f.number("factorId", "id") ?: 0L, f.text("name", "factorName") ?: return@mapNotNull null,
            f.text("reportedStatus") ?: "사용자 확인 필요", true,
            f.text("type") ?: "GOAL", f.text("supported") ?: "UNSUPPORTED", f.text("originalText") ?: "")
    }
}

fun JsonElement?.toPreferences(previous: UserProfile): UserProfile {
    val prefs = unwrap().asObjectOrNull() ?: return previous
    val allergies = prefs.read("allergens", "allergies", "allergenNames")?.asArrayOrNull()?.mapNotNull { el ->
        if (el.isJsonPrimitive) el.asString else el.asObjectOrNull()?.text("allergenName", "name")
    } ?: previous.allergies
    fun csv(name: String, prior: String): String {
        val value = prefs.read(name)
        return value.asArrayOrNull()?.joinToString(", ") { it.asString }
            ?: value?.takeIf { it.isJsonPrimitive }?.asString ?: prior
    }
    val status = prefs.text("allergyStatus") ?: previous.allergyStatus
    return previous.copy(
        allowDiningOut = prefs.flag("allowDiningOut") ?: previous.allowDiningOut,
        allergies = allergies,
        allergyStatus = when(status) { "PRESENT" -> "선택함"; "NONE" -> "없음"; "UNANSWERED" -> "미응답"; else -> status },
        liked = csv("likedFoods", previous.liked), disliked = csv("dislikedFoods", previous.disliked),
        style = csv("dietaryPreferences", previous.style)
    )
}

fun JsonElement?.toUser(previous: UserProfile): UserProfile {
    val root = unwrap().asObjectOrNull() ?: return previous
    return previous.copy(
        email = root.text("email") ?: previous.email,
        name = root.text("name", "nickname") ?: root.text("email")?.substringBefore('@') ?: previous.name,
        age = root.text("age")?.toIntOrNull() ?: previous.age,
        gender = root.text("gender") ?: previous.gender
    )
}

fun JsonElement?.chatReply(): String {
    val root = requiredObject("챗봇")
    return root.text("reply", "message", "content")
        ?: root.read("response").asObjectOrNull()?.text("reply", "content")
        ?: throw IllegalArgumentException("ChatResponse.reply 필드가 없어요")
}

fun JsonElement?.chatSessionId(): Long? {
    val root = unwrap().asObjectOrNull() ?: return null
    return root.number("sessionId", "id") ?: root.read("session").asObjectOrNull()?.number("id")
}

fun JsonElement?.chatMessages(): List<ChatMessage> {
    val raw = unwrap()
    val items = raw.asArrayOrNull()?.toList() ?: raw.asObjectOrNull()?.list("messages", "items", "content") ?: emptyList()
    return items.mapIndexedNotNull { idx, e ->
        val m = e.asObjectOrNull() ?: return@mapIndexedNotNull null
        ChatMessage(m.number("id") ?: idx.toLong(),
            m.text("role")?.uppercase() in setOf("USER", "HUMAN"),
            m.text("content", "message") ?: return@mapIndexedNotNull null)
    }
}

/** The backend stores age and gender separately from /api/users/me. */
fun JsonElement?.toBasicProfile(previous: UserProfile): UserProfile {
    val root = unwrap().asObjectOrNull() ?: return previous
    return previous.copy(
        age = root.text("age")?.toIntOrNull() ?: 0,
        gender = root.text("gender") ?: "선택 안 함"
    )
}
