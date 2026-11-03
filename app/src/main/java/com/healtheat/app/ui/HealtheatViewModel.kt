package com.healtheat.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.healtheat.app.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Real network ViewModel. No fake responses are shown when API calls fail. */
class HealtheatViewModel : ViewModel() {
    val today: LocalDate get() = LocalDate.now(ZoneId.of("Asia/Seoul"))
    private val api = ApiClient.service

    var authenticated by mutableStateOf(false)
        private set
    var authBusy by mutableStateOf(false)
        private set
    var profile by mutableStateOf(UserProfile())
        private set
    var planId by mutableStateOf<Long?>(null)
        private set
    var hasPlan by mutableStateOf(false)
        private set
    var checkingPlan by mutableStateOf(false)
        private set
    var generating by mutableStateOf(false)
        private set
    var recording by mutableStateOf(false)
        private set
    var busyMeal by mutableStateOf<MealType?>(null)
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    var sending by mutableStateOf(false)
        private set
    var analyzing by mutableStateOf(false)
        private set
    var savingProfile by mutableStateOf(false)
        private set
    var loadingRecipe by mutableStateOf(false)
        private set
    var recipeFailure by mutableStateOf<String?>(null)
        private set
    val recipes = mutableStateMapOf<Long, Recipe>()
    var selectedDate by mutableStateOf(today)

    val meals = mutableStateMapOf<MealType, Meal>()
    val previousRecommendations = mutableStateListOf<Meal>()
    val records = mutableStateListOf<MealRecord>()
    val messages = mutableStateListOf<ChatMessage>()
    var sessionId by mutableStateOf<Long?>(null)
        private set
    var chatBusy by mutableStateOf(false)
        private set

    private var nextLocalId = 100000L
    private fun nextId(): Long = ++nextLocalId
    fun toast(text: String) { notice = text }
    fun dismissNotice() { notice = null }

    private fun report(t: Throwable) {
        if (t is CancellationException) throw t
        if (t is ApiException && t.statusCode == 401) {
            authenticated = false
            ApiClient.clearSession()
        }
        toast(t.message ?: "네트워크 연결을 확인해 주세요")
    }

    fun authenticate(email: String, password: String, signup: Boolean, confirm: String, onSuccess: () -> Unit) {
        if (authBusy) return
        if (!email.contains('@') || password.isBlank()) { toast("이메일과 비밀번호를 확인해 주세요"); return }
        if (signup && password.length !in 8..72) { toast("비밀번호는 8~72자로 입력해 주세요"); return }
        if (signup && password != confirm) { toast("비밀번호 확인이 일치하지 않아요"); return }
        authBusy = true
        viewModelScope.launch {
            try {
                // Fail before sending passwords over HTTP. The original backend uses Secure cookies.
                ApiClient.requireHttpsForLogin()
                ApiClient.clearSession()
                if (signup) {
                    ApiClient.prepareMutation()
                    api.signup(obj("email" to email.trim(), "password" to password,
                        "passwordConfirm" to confirm)).requireBodyOrNull()
                }
                ApiClient.prepareMutation()
                api.login(obj("email" to email.trim(), "password" to password)).requireBodyOrNull()
                // Verify cookie authentication and fetch basic profile *before* choosing onboarding.
                val me = api.me().requireBodyOrNull()
                var user = me.toUser(UserProfile()).copy(email = email.trim())
                user = api.basicProfile().requireBodyOrNull().toBasicProfile(user)
                profile = user
                authenticated = true
                // Login can rotate CSRF state; fetch a new token at the next mutation.
                refreshProfile()
                refreshToday()
                refreshRecords()
                toast("${if (signup) "가입하고 " else ""}로그인했어요")
                onSuccess()
            } catch (t: Exception) { report(t) }
            finally { authBusy = false }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            try { ApiClient.prepareMutation(); api.logout().requireBodyOrNull() }
            catch (t: Exception) { report(t) }
            finally {
                ApiClient.clearSession()
                authenticated = false
                profile = UserProfile()
                planId = null; hasPlan = false; meals.clear(); records.clear()
                sessionId = null; messages.clear(); recipes.clear()
                onDone()
            }
        }
    }

    fun refreshProfile() {
        if (!authenticated) return
        viewModelScope.launch {
            try {
                profile = api.me().requireBodyOrNull().toUser(profile)
                profile = api.basicProfile().requireBodyOrNull().toBasicProfile(profile)
                profile = api.preferences().requireBodyOrNull().toPreferences(profile)
                profile = profile.copy(factors = api.healthProfile().requireBodyOrNull().toHealthFactors())
            } catch (t: Exception) { report(t) }
        }
    }

    fun refreshToday() {
        if (!authenticated || checkingPlan) return
        checkingPlan = true
        viewModelScope.launch {
            try {
                val result = api.today()
                val plan = when (result.code()) {
                    204, 404 -> null
                    else -> result.requireBodyOrNull().toPlanOrNull()
                }
                if (plan == null) { hasPlan = false; planId = null; meals.clear() }
                else {
                    planId = plan.first
                    meals.clear(); meals.putAll(plan.second)
                    hasPlan = true
                }
            } catch (t: Exception) { report(t) }
            finally { checkingPlan = false }
        }
    }

    fun generatePlan() {
        if (!authenticated || generating || hasPlan) return
        generating = true
        viewModelScope.launch {
            try {
                ApiClient.prepareMutation()
                val result = api.generate().requireBodyOrNull().toPlanOrNull()
                    ?: throw IllegalArgumentException("생성 응답에 식단이 없어요")
                planId = result.first
                meals.clear(); meals.putAll(result.second)
                hasPlan = true
                toast("오늘의 세 끼 식단이 준비됐어요 ✨")
            } catch (t: Exception) { report(t) }
            finally { generating = false }
        }
    }

    fun regenerate(type: MealType, fromChat: Boolean = false) {
        val currentId = planId ?: run { toast("먼저 식단을 생성해 주세요"); return }
        if (busyMeal != null) return
        busyMeal = type
        viewModelScope.launch {
            try {
                ApiClient.prepareMutation()
                val result = api.regenerate(currentId, type.name, JsonObject()).requireBodyOrNull()
                // Server can respond with a full updated plan or a single meal; always re-read authoritative today plan.
                val changed = result.toSingleMealOrNull()
                val old = meals[type]
                if (old != null && changed != null) previousRecommendations.add(old)
                if (changed != null) meals[type] = changed
                val fresh = api.today().requireBodyOrNull().toPlanOrNull()
                    ?: throw IllegalArgumentException("재추천 이후 식단을 조회할 수 없어요")
                planId = fresh.first
                meals.clear(); meals.putAll(fresh.second)
                if (!fromChat) toast("${type.label} 메뉴를 새로 추천받았어요")
            } catch (t: Exception) { report(t) }
            finally { busyMeal = null }
        }
    }

    fun refreshRecords() {
        if (!authenticated) return
        viewModelScope.launch {
            try {
                val values = api.records(today.minusDays(29).toString(), today.toString())
                    .requireBodyOrNull().toRecords()
                records.clear(); records.addAll(values)
            } catch (t: Exception) { report(t) }
        }
    }

    fun recordRecommended(type: MealType) {
        val meal = meals[type] ?: run { toast("오늘의 식단이 없어요"); return }
        if (records.any { it.date == today && it.type == type }) { toast("이미 기록한 끼니예요"); return }
        if (recording) return
        recording = true
        viewModelScope.launch {
            try {
                ApiClient.prepareMutation()
                api.recordRecommended(obj("recommendationId" to meal.id)).requireBodyOrNull()
                refreshRecords()
                toast("${type.label} 식사 기록을 저장했어요")
            } catch (t: Exception) { report(t); if (t is ApiException && t.statusCode == 409) refreshRecords() }
            finally { recording = false }
        }
    }

    fun recordCustom(type: MealType, foodName: String, date: LocalDate = today, onSuccess: () -> Unit = {}) {
        val cleaned = foodName.trim()
        if (cleaned.isBlank()) { toast("음식 이름을 입력해 주세요"); return }
        if (records.any { it.date == date && it.type == type }) { toast("해당 날짜·끼니는 이미 기록했어요"); return }
        if (recording) return
        recording = true
        viewModelScope.launch {
            try {
                val items = JsonArray().apply { add(obj("foodName" to cleaned)) }
                ApiClient.prepareMutation()
                api.record(obj("mealDate" to date.toString(), "mealType" to type.name,
                    "items" to items)).requireBodyOrNull()
                refreshRecords()
                toast("식사 기록을 저장했어요")
                onSuccess()
            } catch (t: Exception) { report(t); if (t is ApiException && t.statusCode == 409) refreshRecords() }
            finally { recording = false }
        }
    }

    fun updateDiningOut(value: Boolean) {
        viewModelScope.launch {
            try {
                ApiClient.prepareMutation()
                api.savePreferences(obj("allowDiningOut" to value)).requireBodyOrNull()
                profile = profile.copy(allowDiningOut = value)
                toast("다음 재추천부터 설정이 반영돼요")
            } catch (t: Exception) { report(t) }
        }
    }

    fun analyzeHealth(rawText: String, onResult: (List<HealthFactor>) -> Unit) {
        if (analyzing || rawText.isBlank()) return
        analyzing = true
        viewModelScope.launch {
            try {
                ApiClient.prepareMutation()
                val factors = api.analyze(obj("rawText" to rawText.trim()))
                    .requireBodyOrNull().toCandidateFactors()
                onResult(factors)
            } catch (t: Exception) { report(t) }
            finally { analyzing = false }
        }
    }

    fun updateProfile(
        age: Int, gender: String, diningOut: Boolean, healthText: String,
        allergies: List<String>, allergyStatus: String, liked: String, disliked: String,
        style: String, factors: List<HealthFactor>, onSuccess: () -> Unit = {}
    ) {
        if (savingProfile) return
        if (allergyStatus == "선택함" && allergies.isEmpty()) { toast("알레르기를 하나 이상 선택해 주세요"); return }
        savingProfile = true
        viewModelScope.launch {
            try {
                val rawStatus = when(allergyStatus) { "선택함" -> "PRESENT"; "없음" -> "NONE"; else -> "UNANSWERED" }
                val asArray = { values: List<String> -> JsonArray().apply { values.forEach { add(it) } } }
                // Save server-side age and gender; they live in their own endpoint.
                ApiClient.prepareMutation()
                api.saveBasicProfile(obj("age" to age,
                    "gender" to gender.takeUnless { it == "선택 안 함" })).requireBodyOrNull()
                ApiClient.prepareMutation()
                api.savePreferences(obj(
                    "allowDiningOut" to diningOut, "allergyStatus" to rawStatus,
                    "allergens" to asArray(allergies),
                    "likedFoods" to asArray(liked.split(',').map { it.trim() }.filter { it.isNotEmpty() }),
                    "dislikedFoods" to asArray(disliked.split(',').map { it.trim() }.filter { it.isNotEmpty() }),
                    "dietaryPreferences" to asArray(style.split(',').map { it.trim() }.filter { it.isNotEmpty() })
                )).requireBodyOrNull()
                if (healthText.isNotBlank()) {
                    // PUT is a full replacement: preserve existing factors unless explicitly changed.
                    // The backend PUT is full replacement; retain active AND inactive factors.
                    // Distinguish same names with different factor types. New confirmed factors override matches.
                    val combined = (profile.factors + factors)
                        .associateBy { it.label.trim().lowercase() to it.type.trim().lowercase() }
                        .values.toList()
                    val candidates = JsonArray().apply {
                        combined.forEach { f -> add(obj(
                            "factorId" to f.id.takeIf { it > 0 },
                            "name" to f.label,
                            "type" to f.type,
                            "reportedStatus" to f.status,
                            "originalText" to f.originalText.takeIf { it.isNotBlank() },
                            "active" to f.isActive
                        )) }
                    }
                    ApiClient.prepareMutation()
                    api.saveHealth(obj("factors" to candidates)).requireBodyOrNull()
                }
                profile = profile.copy(age = age, gender = gender, allowDiningOut = diningOut,
                    allergies = allergies, allergyStatus = allergyStatus, liked = liked, disliked = disliked,
                    style = style, factors = if (healthText.isNotBlank())
                        (profile.factors + factors).associateBy { it.label.trim().lowercase() to it.type.trim().lowercase() }.values.toList()
                    else profile.factors)
                refreshProfile()
                toast("건강 프로필을 서버에 저장했어요")
                onSuccess()
            } catch (t: Exception) { report(t) }
            finally { savingProfile = false }
        }
    }

    fun fetchRecipe(item: FoodItem) {
        val recipeId = item.recipeId ?: return
        if (recipes.containsKey(recipeId)) return
        loadingRecipe = true; recipeFailure = null
        viewModelScope.launch {
            try { recipes[recipeId] = api.recipe(recipeId).requireBodyOrNull().toRecipe() }
            catch (t: Exception) { recipeFailure = t.message ?: "레시피 조회 실패" }
            finally { loadingRecipe = false }
        }
    }

    fun openChat() {
        if (!authenticated || chatBusy || sessionId != null) return
        chatBusy = true
        viewModelScope.launch {
            try {
                ApiClient.prepareMutation()
                val session = api.startChat().requireBodyOrNull().chatSessionId()
                    ?: throw IllegalArgumentException("채팅 응답에서 sessionId를 찾지 못했어요")
                sessionId = session
                val history = api.chatHistory(session).requireBodyOrNull().chatMessages()
                messages.clear(); messages.addAll(history)
                if (history.isEmpty()) messages.add(ChatMessage(nextId(), false,
                    "안녕하세요! 음식 상담, 식사 횟수 조회, 건강정보 변경, 식단 재추천을 도와드릴게요."))
            } catch (t: Exception) { report(t) }
            finally { chatBusy = false }
        }
    }

    fun sendMessage(text: String) {
        val message = text.trim()
        val session = sessionId ?: run { toast("채팅 세션을 시작해 주세요"); openChat(); return }
        if (message.isEmpty() || sending) return
        sending = true
        messages.add(ChatMessage(nextId(), true, message))
        viewModelScope.launch {
            try {
                ApiClient.prepareMutation()
                val result = api.send(obj("sessionId" to session, "message" to message)).requireBodyOrNull()
                messages.add(ChatMessage(nextId(), false, result.chatReply()))
                // Chat can alter meal plan / health data on server without an approval step.
                refreshProfile(); refreshToday()
            } catch (t: Exception) {
                messages.add(ChatMessage(nextId(), false, "전송 실패: ${t.message ?: "서버 연결을 확인해 주세요"}"))
                if (t is ApiException && t.statusCode == 410) sessionId = null
                report(t)
            } finally { sending = false }
        }
    }

    fun endChat() {
        val id = sessionId ?: return
        viewModelScope.launch {
            try {
                ApiClient.prepareMutation()
                api.endChat(id).requireBodyOrNull()
                sessionId = null; messages.clear()
                toast("채팅을 종료하고 대화 내역을 삭제했어요")
            } catch (t: Exception) { report(t) }
        }
    }

}
