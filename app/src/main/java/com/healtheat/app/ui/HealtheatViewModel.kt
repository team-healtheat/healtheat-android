package com.healtheat.app.ui

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.healtheat.app.data.ApiClient
import com.healtheat.app.data.ApiException
import com.healtheat.app.data.ChatMessage
import com.healtheat.app.data.FoodItem
import com.healtheat.app.data.HealthFactor
import com.healtheat.app.data.Meal
import com.healtheat.app.data.MealRecord
import com.healtheat.app.data.MealType
import com.healtheat.app.data.Recipe
import com.healtheat.app.data.UserProfile
import com.healtheat.app.data.chatMessages
import com.healtheat.app.data.chatReply
import com.healtheat.app.data.chatSessionId
import com.healtheat.app.data.obj
import com.healtheat.app.data.requireBodyOrNull
import com.healtheat.app.data.toBasicProfile
import com.healtheat.app.data.toCandidateFactors
import com.healtheat.app.data.toHealthFactors
import com.healtheat.app.data.toPlanOrNull
import com.healtheat.app.data.toPreferences
import com.healtheat.app.data.toRecipe
import com.healtheat.app.data.toRecords
import com.healtheat.app.data.toSingleMealOrNull
import com.healtheat.app.data.toUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Real network ViewModel. No fake responses are shown when API calls fail. */
class HealtheatViewModel : ViewModel() {
    private enum class GuidedChatIntent { FOOD_SAFETY, FOOD_FREQUENCY }

    val today: LocalDate get() = LocalDate.now(ZoneId.of("Asia/Seoul"))
    private val api = ApiClient.service

    var authenticated by mutableStateOf(false)
        private set
    var restoringSession by mutableStateOf(true)
        private set
    var authBusy by mutableStateOf(false)
        private set
    var updatingNickname by mutableStateOf(false)
        private set
    var deletingAccount by mutableStateOf(false)
        private set
    var deleteAccountError by mutableStateOf<String?>(null)
        private set

    fun clearDeleteAccountError() {
        deleteAccountError = null
    }
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
    val calendarRecords = mutableStateListOf<MealRecord>()
    var calendarLoading by mutableStateOf(false)
        private set
    var calendarError by mutableStateOf<String?>(null)
        private set
    private var calendarJob: Job? = null
    private var calendarLoadVersion = 0
    val messages = mutableStateListOf<ChatMessage>()
    var sessionId by mutableStateOf<Long?>(null)
        private set
    var chatBusy by mutableStateOf(false)
        private set
    private var guidedChatIntent: GuidedChatIntent? = null
    private var guidedFoodName: String? = null
    private var pendingChatExampleIndex: Int? = null

    private var nextLocalId = -1L
    private fun nextId(): Long = nextLocalId--

    private fun clearAccountData() {
        profile = UserProfile()

        // 식단
        planId = null
        hasPlan = false
        meals.clear()
        previousRecommendations.clear()

        // 식사 기록
        records.clear()
        calendarJob?.cancel()
        calendarRecords.clear()
        calendarLoading = false
        calendarError = null
        calendarLoadVersion++
        selectedDate = today

        // 채팅
        sessionId = null
        messages.clear()
        clearGuidedChat()

        // 레시피
        recipes.clear()
        recipeFailure = null

        // 이전 요청 상태
        checkingPlan = false
        generating = false
        recording = false
        busyMeal = null
        chatBusy = false
        sending = false
        analyzing = false
        savingProfile = false
        loadingRecipe = false
    }

    fun toast(text: String) {
        notice = text
    }

    fun dismissNotice() {
        notice = null
    }

    private fun report(t: Throwable) {
        if (t is CancellationException) throw t
        if (t is ApiException && t.statusCode == 401) {
            authenticated = false
            ApiClient.clearSession()
        }
        val friendly = when (t) {
            is ApiException -> when (t.statusCode) {
                400 -> "입력한 내용을 확인해 주세요."
                401 -> "로그인이 만료됐어요. 다시 로그인해 주세요."
                403 -> "요청을 처리할 수 없어요. 다시 시도해 주세요."
                404 -> "요청한 정보를 찾지 못했어요."
                409 -> "이미 등록된 내용이 있어요. 확인해 주세요."
                429 -> "요청이 많아요. 잠시 후 다시 시도해 주세요."
                in 500..599 -> "서버가 응답하지 않아요. 잠시 후 다시 시도해 주세요."
                else -> "요청에 실패했어요. 다시 시도해 주세요."
            }
            else -> "연결 상태를 확인하고 다시 시도해 주세요."
        }
        toast(friendly)
    }

    fun authenticate(
        email: String,
        password: String,
        signup: Boolean,
        confirm: String,
        nickname: String = "",
        onSuccess: () -> Unit
    ) {
        if (authBusy) return
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            toast("올바른 이메일 주소를 입력해 주세요.")
            return
        }

        if (password.isBlank()) {
            toast("비밀번호를 입력해 주세요.")
            return
        }
        if (signup && password.length !in 8..72) {
            toast("비밀번호는 8자 이상 입력해 주세요."); return
        }

        if (signup && password != confirm) {
            toast("비밀번호 확인이 일치하지 않아요"); return
        }
        if (signup && nickname.trim().length !in 1..50) {
            toast("닉네임은 1~50자로 입력해 주세요.")
            return
        }
        // 이전 계정의 진행 중인 API 요청 취소
        viewModelScope.coroutineContext.cancelChildren()

        // 이전 계정 정보 제거
        authenticated = false
        clearAccountData()

        authBusy = true
        viewModelScope.launch {
            try {
                // Fail before sending passwords over HTTP. The original backend uses Secure cookies.
                ApiClient.requireHttpsForLogin()
                ApiClient.clearSession()
                if (signup) {
                    ApiClient.prepareMutation()
                    api.signup(
                        obj(
                            "email" to email.trim(),
                            "password" to password,
                            "passwordConfirm" to confirm,
                            "nickname" to nickname.trim()
                        )
                    ).requireBodyOrNull()
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
            } catch (t: Exception) {
                if (t is CancellationException) throw t
                if (t is ApiException) {
                    when {
                        signup && t.statusCode == 409 ->
                            toast("이미 사용 중인 이메일입니다.")
                        !signup && (t.statusCode == 400 || t.statusCode == 401) ->
                            toast("이메일 또는 비밀번호를 확인해 주세요.")
                        else -> report(t)
                    }
                } else report(t)
            } finally {
                authBusy = false
            }
        }
    }

    fun restoreSession(onRestored: () -> Unit) {
        viewModelScope.launch {
            try {
                // 저장된 인증 쿠키가 없으면 로그인 화면 유지
                if (!ApiClient.cookies.hasRestorableSession()) {
                    return@launch
                }

                // 기존 Access Token으로 먼저 인증 확인
                val firstResponse = api.me()

                val me = if (firstResponse.code() == 401) {
                    // Access Token 만료 시 Refresh Token으로 갱신
                    ApiClient.prepareMutation()
                    api.refreshToken().requireBodyOrNull()

                    // 갱신된 Access Token으로 다시 확인
                    api.me().requireBodyOrNull()
                } else {
                    firstResponse.requireBodyOrNull()
                }

                // 실제 서버에서 확인된 사용자만 로그인 처리
                val user = me.toUser(UserProfile())

                profile = api.basicProfile()
                    .requireBodyOrNull()
                    .toBasicProfile(user)

                authenticated = true

                refreshProfile()
                onRestored()

            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.statusCode == 401) {
                    // Refresh Token까지 만료된 경우
                    ApiClient.clearSession()
                }
            } catch (_: Exception) {
                // 일시적인 네트워크 오류에서는
                // 저장된 인증 쿠키를 삭제하지 않음
            } finally {
                restoringSession = false
            }
        }
    }


    fun logout(onDone: () -> Unit) {
        // 이전 계정에서 실행 중이던 요청 취소
        viewModelScope.coroutineContext.cancelChildren()

        viewModelScope.launch {
            try {
                ApiClient.prepareMutation()
                api.logout().requireBodyOrNull()
            } catch (t: Exception) {
                if (t is CancellationException) throw t
                report(t)
            } finally {
                // 로컬 인증 정보와 사용자 데이터 삭제
                ApiClient.clearSession()
                authenticated = false
                clearAccountData()

                onDone()
            }
        }
    }



    fun deleteAccount(
        password: String,
        onSuccess: () -> Unit
    ) {
        if (!authenticated || deletingAccount) return

        deleteAccountError = null

        if (password.isBlank()) {
            deleteAccountError = "현재 비밀번호를 입력해 주세요."
            return
        }

        deletingAccount = true

        viewModelScope.launch {
            try {
                ApiClient.prepareMutation()

                api.deleteAccount(
                    obj("password" to password)
                ).requireBodyOrNull()

                // 서버가 탈퇴 성공을 반환한 경우에만 세션 삭제
                ApiClient.clearSession()
                authenticated = false
                clearAccountData()

                toast("회원탈퇴가 완료됐어요.")
                onSuccess()

            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                // 회원탈퇴의 인증 실패를 공통 로그인 만료로 처리하지 않음
                deleteAccountError = when (e.statusCode) {
                    400 -> "입력한 비밀번호를 확인해 주세요."
                    401 -> {
                        // 실제 세션 만료와 비밀번호 불일치 구분
                        val sessionValid = try {
                            api.me().isSuccessful
                        } catch (_: Exception) {
                            null
                        }

                        when (sessionValid) {
                            true -> "비밀번호가 올바르지 않아요. 다시 시도해 주세요."
                            false -> "로그인 세션이 만료됐어요. 다시 로그인해 주세요."
                            null -> "인증 상태를 확인할 수 없어요. 다시 시도해 주세요."
                        }
                    }
                    403 -> "보안 확인에 실패했어요. 다시 시도해 주세요."
                    else -> e.message
                }
            } catch (e: Exception) {
                deleteAccountError =
                    "네트워크 연결을 확인하고 다시 시도해 주세요."
            } finally {
                deletingAccount = false
            }
        }
    }


    fun updateNickname(
        newNickname: String,
        onSuccess: () -> Unit = {}
    ) {
        val nickname = newNickname.trim()

        if (nickname.length !in 1..50) {
            toast("닉네임은 1~50자로 입력해 주세요.")
            return
        }

        if (!authenticated || updatingNickname) return

        updatingNickname = true

        viewModelScope.launch {
            try {
                ApiClient.prepareMutation()

                val result = api.updateNickname(
                    obj("nickname" to nickname)
                ).requireBodyOrNull()

                // 서버에서 변경한 닉네임을 화면에 반영
                profile = result.toUser(profile)

                toast("닉네임이 변경됐어요.")
                onSuccess()

            } catch (t: CancellationException) {
                throw t
            } catch (t: Exception) {
                report(t)
            } finally {
                updatingNickname = false
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
                profile = profile.copy(
                    factors = api.healthProfile().requireBodyOrNull().toHealthFactors()
                )
            } catch (t: Exception) {
                report(t)
            }
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
                if (plan == null) {
                    hasPlan = false; planId = null; meals.clear()
                } else {
                    planId = plan.first
                    meals.clear(); meals.putAll(plan.second)
                    hasPlan = true
                }
            } catch (t: Exception) {
                report(t)
            } finally {
                checkingPlan = false
            }
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
                toast("오늘의 식단이 준비됐어요")
            } catch (t: Exception) {
                report(t)
            } finally {
                generating = false
            }
        }
    }

    fun regenerate(type: MealType, fromChat: Boolean = false) {
        val currentId = planId ?: run { toast("먼저 식단을 생성해 주세요"); return }
        if (busyMeal != null) return
        busyMeal = type
        viewModelScope.launch {
            try {
                ApiClient.prepareMutation()
                // 서버에서 재추천이 끝난 뒤 최신 식단을 조회한다.
                // 중간 응답으로 카드를 먼저 바꾸지 않아 화면이 두 번 재구성되지 않도록 한다.
                api.regenerate(currentId, type.name, JsonObject()).requireBodyOrNull()
                val fresh = api.today().requireBodyOrNull().toPlanOrNull()
                    ?: throw IllegalArgumentException("재추천 이후 식단을 조회할 수 없어요")
                val old = meals[type]
                if (old != null && fresh.second[type] != null) previousRecommendations.add(old)
                planId = fresh.first
                meals.putAll(fresh.second)
                meals.keys.toList().filter { it !in fresh.second.keys }.forEach { meals.remove(it) }
                if (!fromChat) toast("${type.label} 메뉴를 새로 추천받았어요")
            } catch (t: Exception) {
                report(t)
            } finally {
                busyMeal = null
            }
        }
    }

    /** 캘린더는 선택된 월만 별도 조회한다. 홈의 오늘 기록 목록과 섞이지 않는다. */
    fun loadCalendarRecords(month: YearMonth) {
        if (!authenticated) return
        calendarJob?.cancel()
        val version = ++calendarLoadVersion
        calendarLoading = true
        calendarError = null
        calendarRecords.clear()
        calendarJob = viewModelScope.launch {
            try {
                val values = api.records(
                    month.atDay(1).toString(),
                    month.atEndOfMonth().toString()
                ).requireBodyOrNull().toRecords()
                if (version == calendarLoadVersion) {
                    calendarRecords.addAll(values)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (version == calendarLoadVersion) {
                    calendarError = "식사 기록을 불러오지 못했어요. 다시 시도해 주세요."
                }
            } finally {
                if (version == calendarLoadVersion) calendarLoading = false
            }
        }
    }

    fun refreshRecords() {
        if (!authenticated) return
        viewModelScope.launch {
            try {
                val values = api.records(today.minusDays(29).toString(), today.toString())
                    .requireBodyOrNull().toRecords()
                records.clear(); records.addAll(values)
            } catch (t: Exception) {
                report(t)
            }
        }
    }

    fun recordRecommended(type: MealType) {
        val meal = meals[type] ?: run { toast("오늘의 식단이 없어요"); return }
        if (records.any { it.date == today && it.type == type }) {
            toast("이미 기록한 끼니예요"); return
        }
        if (recording) return
        recording = true
        viewModelScope.launch {
            try {
                ApiClient.prepareMutation()
                api.recordRecommended(obj("recommendationId" to meal.id)).requireBodyOrNull()
                refreshRecords()
                toast("${type.label} 식사 기록을 저장했어요")
            } catch (t: Exception) {
                report(t); if (t is ApiException && t.statusCode == 409) refreshRecords()
            } finally {
                recording = false
            }
        }
    }

    fun recordCustom(
        type: MealType,
        foodName: String,
        date: LocalDate = today,
        onSuccess: () -> Unit = {}
    ) {
        val cleaned = foodName.trim()
        if (cleaned.isBlank()) {
            toast("음식 이름을 입력해 주세요"); return
        }
        if (records.any { it.date == date && it.type == type }) {
            toast("해당 날짜·끼니는 이미 기록했어요"); return
        }
        if (recording) return
        recording = true
        viewModelScope.launch {
            try {
                val items = JsonArray().apply { add(obj("foodName" to cleaned)) }
                ApiClient.prepareMutation()
                api.record(
                    obj(
                        "mealDate" to date.toString(), "mealType" to type.name,
                        "items" to items
                    )
                ).requireBodyOrNull()
                refreshRecords()
                toast("식사 기록을 저장했어요")
                onSuccess()
            } catch (t: Exception) {
                report(t); if (t is ApiException && t.statusCode == 409) refreshRecords()
            } finally {
                recording = false
            }
        }
    }

    /** Calendar records can be corrected without changing the recommendation flow. */
    fun updateCustomRecord(
        recordId: Long,
        type: MealType,
        foodName: String,
        date: LocalDate,
        onSuccess: () -> Unit = {}
    ) {
        val cleaned = foodName.trim()
        if (cleaned.isBlank()) {
            toast("음식 이름을 입력해 주세요"); return
        }
        if (recording) return
        recording = true
        viewModelScope.launch {
            try {
                val items = JsonArray().apply { add(obj("foodName" to cleaned)) }
                ApiClient.prepareMutation()
                api.updateMealRecord(
                    recordId,
                    obj(
                        "mealDate" to date.toString(), "mealType" to type.name,
                        "items" to items
                    )
                ).requireBodyOrNull()
                refreshRecords()
                toast("식사 기록을 수정했어요")
                onSuccess()
            } catch (t: Exception) {
                report(t)
            } finally {
                recording = false
            }
        }
    }

    /** 기본 정보 PUT은 전체 교체 방식이므로 바꾸지 않은 값도 함께 보존해 전송한다. */
    fun saveBasicInfo(
        age: Int,
        gender: String,
        onSuccess: () -> Unit = {}
    ) {
        if (!authenticated || savingProfile) return
        if (age !in 1..120) {
            toast("나이는 1~120세 사이로 입력해 주세요.")
            return
        }
        savingProfile = true
        viewModelScope.launch {
            try {
                ApiClient.prepareMutation()
                api.saveBasicProfile(
                    obj(
                        "age" to age,
                        "gender" to gender.takeUnless { it == "선택 안 함" }
                    )
                ).requireBodyOrNull()
                profile = profile.copy(age = age, gender = gender)
                toast("기본 정보가 변경됐어요.")
                onSuccess()
            } catch (t: Exception) {
                report(t)
            } finally {
                savingProfile = false
            }
        }
    }

    /** 음식 취향은 PATCH로 해당 필드만 전송하며 다른 설정은 수정하지 않는다. */
    fun saveFoodPreference(
        field: String,
        rawText: String,
        onSuccess: () -> Unit = {}
    ) {
        if (!authenticated || savingProfile) return
        if (field !in setOf("likedFoods", "dislikedFoods", "dietaryPreferences")) {
            toast("수정할 수 없는 항목이에요.")
            return
        }
        val values = rawText.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        if (values.size > 50 || values.any { it.length > 200 }) {
            toast("항목은 최대 50개, 각 항목은 200자 이내로 입력해 주세요.")
            return
        }
        savingProfile = true
        viewModelScope.launch {
            try {
                val valuesJson = JsonArray().apply { values.forEach { add(it) } }
                ApiClient.prepareMutation()
                api.savePreferences(obj(field to valuesJson)).requireBodyOrNull()
                val displayed = values.joinToString(", ")
                profile = when (field) {
                    "likedFoods" -> profile.copy(liked = displayed)
                    "dislikedFoods" -> profile.copy(disliked = displayed)
                    else -> profile.copy(style = displayed)
                }
                toast("음식 취향이 변경됐어요.")
                onSuccess()
            } catch (t: Exception) {
                report(t)
            } finally {
                savingProfile = false
            }
        }
    }

    /** 알레르기는 서버 명세대로 상태와 목록을 한 번에 PATCH한다. */
    fun saveAllergyPreference(
        status: String,
        allergens: List<String>,
        onSuccess: () -> Unit = {}
    ) {
        if (!authenticated || savingProfile) return
        if (status !in setOf("있음", "없음")) {
            toast("알레르기 유무를 선택해 주세요.")
            return
        }
        val names = if (status == "있음") allergens
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        else emptyList()
        if (status == "있음" && names.isEmpty()) {
            toast("알레르기를 하나 이상 선택해 주세요.")
            return
        }
        if (names.size > 50 || names.any { it.length > 200 }) {
            toast("알레르기는 최대 50개, 각 항목은 200자 이내로 입력해 주세요.")
            return
        }
        savingProfile = true
        viewModelScope.launch {
            try {
                val namesJson = JsonArray().apply { names.forEach { add(it) } }
                ApiClient.prepareMutation()
                api.savePreferences(
                    obj(
                        "allergyStatus" to if (status == "있음") "PRESENT" else "NONE",
                        "allergens" to namesJson
                    )
                ).requireBodyOrNull()
                profile = profile.copy(allergyStatus = status, allergies = names)
                toast("알레르기 정보가 변경됐어요.")
                onSuccess()
            } catch (t: Exception) {
                report(t)
            } finally {
                savingProfile = false
            }
        }
    }

    fun updateDiningOut(value: Boolean) {
        viewModelScope.launch {
            try {
                ApiClient.prepareMutation()
                api.savePreferences(obj("allowDiningOut" to value)).requireBodyOrNull()
                profile = profile.copy(allowDiningOut = value)
                toast("다음 재추천부터 설정이 반영돼요")
            } catch (t: Exception) {
                report(t)
            }
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
            } catch (t: Exception) {
                report(t)
            } finally {
                analyzing = false
            }
        }
    }

    private fun healthFactorPayload(choices: List<HealthFactor>): JsonObject {
        // The server PUT replaces the active set. Keep unrelated factor types active,
        // and let deselected diagnoses become inactive by omitting them.
        val unrelated = profile.factors.filter {
            it.isActive && !it.type.equals("DIAGNOSIS", ignoreCase = true)
        }
        val activeDiagnoses = choices.filter { it.isActive }
        val allActive = (unrelated + activeDiagnoses).distinctBy {
            it.label.trim() to it.type.trim()
        }
        val payload = JsonArray()
        allActive.forEach { factor ->
            val previous = profile.factors.firstOrNull {
                it.label.equals(factor.label.trim(), ignoreCase = true) &&
                        it.type.equals(factor.type.trim(), ignoreCase = true)
            }
            payload.add(obj(
                "factorId" to (previous?.id ?: factor.id).takeIf { it > 0 },
                "name" to factor.label.trim(),
                "type" to factor.type,
                "reportedStatus" to factor.status.ifBlank { "진단 받음" },
                "originalText" to factor.originalText.takeIf { it.isNotBlank() },
                "active" to true
            ))
        }
        return obj("factors" to payload)
    }

    /** 마이페이지의 건강요인 편집만 수행하며 나이·알레르기·취향은 변경하지 않는다. */
    fun saveSelectedHealthFactors(
        factors: List<HealthFactor>,
        onSuccess: () -> Unit = {}
    ) {
        if (!authenticated || savingProfile) return
        savingProfile = true
        viewModelScope.launch {
            try {
                ApiClient.prepareMutation()
                val saved = api.saveHealth(healthFactorPayload(factors))
                    .requireBodyOrNull().toHealthFactors()
                profile = profile.copy(factors = saved)
                toast("건강 요인이 변경됐어요.")
                onSuccess()
            } catch (t: Exception) {
                report(t)
            } finally {
                savingProfile = false
            }
        }
    }

    fun updateProfile(
        age: Int, gender: String, diningOut: Boolean, healthText: String,
        allergies: List<String>, allergyStatus: String, liked: String, disliked: String,
        style: String, factors: List<HealthFactor>, onSuccess: () -> Unit = {}
    ) {
        if (savingProfile) return
        if (allergyStatus == "있음" && allergies.isEmpty()) {
            toast("알레르기를 하나 이상 선택해 주세요"); return
        }
        savingProfile = true
        viewModelScope.launch {
            try {
                val rawStatus = when (allergyStatus) {
                    "있음" -> "PRESENT"; "없음" -> "NONE"; else -> "UNANSWERED"
                }
                val asArray =
                    { values: List<String> -> JsonArray().apply { values.forEach { add(it) } } }
                // Save server-side age and gender; they live in their own endpoint.
                ApiClient.prepareMutation()
                api.saveBasicProfile(
                    obj(
                        "age" to age,
                        "gender" to gender.takeUnless { it == "선택 안 함" })
                ).requireBodyOrNull()
                ApiClient.prepareMutation()
                api.savePreferences(
                    obj(
                        "allowDiningOut" to diningOut, "allergyStatus" to rawStatus,
                        "allergens" to asArray(allergies),
                        "likedFoods" to asArray(liked.split(',').map { it.trim() }
                            .filter { it.isNotEmpty() }),
                        "dislikedFoods" to asArray(disliked.split(',').map { it.trim() }
                            .filter { it.isNotEmpty() }),
                        "dietaryPreferences" to asArray(style.split(',').map { it.trim() }
                            .filter { it.isNotEmpty() })
                    )
                ).requireBodyOrNull()
                // 기존 AI 추출 대신 이용자가 선택한 질환의 전체 활성 목록을 저장.
                // 빈 목록(없음)은 기존 항목 전체를 비활성화한다.
                ApiClient.prepareMutation()
                val savedFactors = api.saveHealth(healthFactorPayload(factors))
                    .requireBodyOrNull().toHealthFactors()
                profile = profile.copy(
                    age = age,
                    gender = gender,
                    allowDiningOut = diningOut,
                    allergies = allergies,
                    allergyStatus = allergyStatus,
                    liked = liked,
                    disliked = disliked,
                    style = style,
                    factors = savedFactors
                )
                refreshProfile()
                toast("건강 정보가 저장됐어요.")
                onSuccess()
            } catch (t: Exception) {
                report(t)
            } finally {
                savingProfile = false
            }
        }
    }

    fun fetchRecipe(item: FoodItem) {
        val recipeId = item.recipeId ?: return
        if (recipes.containsKey(recipeId)) return
        loadingRecipe = true; recipeFailure = null
        viewModelScope.launch {
            try {
                recipes[recipeId] = api.recipe(recipeId).requireBodyOrNull().toRecipe()
            } catch (t: Exception) {
                recipeFailure = t.message ?: "레시피 조회 실패"
            } finally {
                loadingRecipe = false
            }
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
                val history = try {
                    api.chatHistory(session).requireBodyOrNull().chatMessages()
                } catch (_: Exception) {
                    emptyList()
                }
                messages.clear(); messages.addAll(history)
                if (history.isEmpty()) messages.add(
                    ChatMessage(
                        nextId(), false,
                        "안녕하세요! 음식에 대한 궁금증이나 식사 기록을 편하게 물어보세요."
                    )
                )
                pendingChatExampleIndex?.let { index ->
                    pendingChatExampleIndex = null
                    selectChatExample(index)
                }
            } catch (t: Exception) {
                report(t)
            } finally {
                chatBusy = false
            }
        }
    }

    /** Shows an input guide for a suggestion chip without sending an API request. */
    fun selectChatExample(index: Int) {
        if (sessionId == null) {
            pendingChatExampleIndex = index
            openChat()
            return
        }
        if (sending) return
        val intentAndPrompt = when (index) {
            0 -> GuidedChatIntent.FOOD_SAFETY to "어떤 음식을 먹어도 되는지 궁금하신가요? 음식 이름을 알려주세요!"
            1 -> GuidedChatIntent.FOOD_FREQUENCY to "확인할 음식 이름을 알려주세요!"
            else -> return
        }
        guidedChatIntent = intentAndPrompt.first
        guidedFoodName = null
        messages.add(ChatMessage(nextId(), false, intentAndPrompt.second))
    }

    fun sendMessage(text: String) {
        val message = text.trim()
        val session = sessionId ?: run { toast("채팅 세션을 시작해 주세요"); openChat(); return }
        if (message.isEmpty() || sending) return
        messages.add(ChatMessage(nextId(), true, message))
        val apiMessage = completeGuidedMessage(message) ?: return
        sending = true
        viewModelScope.launch {
            try {
                ApiClient.prepareMutation()
                val result = api.send(obj("sessionId" to session, "message" to apiMessage))
                    .requireBodyOrNull()
                messages.add(ChatMessage(nextId(), false, result.chatReply()))
                // Chat can alter meal plan / health data on server without an approval step.
                refreshProfile(); refreshToday()
            } catch (t: Exception) {
                if (t is CancellationException) throw t
                messages.add(
                    ChatMessage(
                        nextId(),
                        false,
                        "답변을 받지 못했어요. 잠시 후 다시 시도해 주세요."
                    )
                )
                if (t is ApiException && t.statusCode == 410) {
                    sessionId = null
                    clearGuidedChat()
                }
                report(t)
            } finally {
                sending = false
            }
        }
    }

    private fun completeGuidedMessage(input: String): String? = when (guidedChatIntent) {
        null -> input
        GuidedChatIntent.FOOD_SAFETY -> {
            clearGuidedChat()
            "${input}${objectParticle(input)} 먹어도 될까?"
        }

        GuidedChatIntent.FOOD_FREQUENCY -> {
            val food = guidedFoodName
            if (food == null) {
                guidedFoodName = input
                messages.add(
                    ChatMessage(
                        nextId(),
                        false,
                        "어느 기간의 식사 횟수를 확인할까요? 예: 최근 2주, 10월 1일~10월 31일"
                    )
                )
                null
            } else {
                clearGuidedChat()
                "${food}${objectParticle(food)} ${input} 동안 얼마나 자주 먹었을까?"
            }
        }

    }

    private fun objectParticle(word: String): String {
        val last = word.lastOrNull() ?: return "을"
        if (last !in '\uAC00'..'\uD7A3') return "을"
        return if ((last.code - 0xAC00) % 28 == 0) "를" else "을"
    }

    private fun clearGuidedChat() {
        guidedChatIntent = null
        guidedFoodName = null
        pendingChatExampleIndex = null
    }

    fun endChat() {
        val id = sessionId ?: return
        if (chatBusy) return
        chatBusy = true
        viewModelScope.launch {
            var startedNewSession = false
            try {
                ApiClient.prepareMutation()
                api.endChat(id).requireBodyOrNull()
                sessionId = null; messages.clear(); clearGuidedChat()
                // A fresh session follows immediately, so the empty state is never mistaken for a failure.
                chatBusy = false
                openChat()
                startedNewSession = true
            } catch (t: Exception) {
                report(t)
            } finally {
                if (!startedNewSession) chatBusy = false
            }
        }
    }

}
