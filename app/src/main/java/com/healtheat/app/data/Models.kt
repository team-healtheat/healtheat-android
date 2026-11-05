package com.healtheat.app.data

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class MealType(val label: String, val emoji: String) {
    BREAKFAST("아침", "🥐"), LUNCH("점심", "🥗"), DINNER("저녁", "🍲")
}

enum class SourceType { HOME_COOKED, DINING_OUT }

enum class PreparationType { COOK, PREPARE, READY_TO_EAT }

data class Recipe(
    val title: String,
    val minutes: Int,
    val ingredients: List<String>,
    val steps: List<String>
)

data class FoodItem(
    val id: Long,
    val name: String,
    val serving: String,
    val preparation: PreparationType,
    val recipe: Recipe? = null,
    val recipeId: Long? = null
)

data class Meal(
    val id: Long,
    val type: MealType,
    val source: SourceType,
    val headline: String,
    val reason: String,
    val items: List<FoodItem>,
    val version: Int = 1
)

data class HealthFactor(
    val id: Long,
    val label: String,
    val status: String,
    val isActive: Boolean = true,
    val type: String = "GOAL",
    val supported: String = "UNSUPPORTED",
    val originalText: String = ""
)

data class UserProfile(
    val name: String = "사용자",
    val email: String = "",
    val age: Int = 0,
    val gender: String = "선택 안 함",
    val allowDiningOut: Boolean = true,
    val factors: List<HealthFactor> = emptyList(),
    val allergies: List<String> = emptyList(),
    val allergyStatus: String = "미응답",
    val liked: String = "",
    val disliked: String = "",
    val style: String = ""
)

data class MealRecord(
    val id: Long,
    val date: LocalDate,
    val type: MealType,
    val foods: List<String>,
    val fromRecommendation: Boolean,
    val recommendationId: Long? = null
)

data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val time: String = ZonedDateTime
        .now(ZoneId.of("Asia/Seoul"))
        .format(DateTimeFormatter.ofPattern("M/d HH:mm"))
)


