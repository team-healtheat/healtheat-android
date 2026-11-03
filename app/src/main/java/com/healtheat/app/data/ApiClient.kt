package com.healtheat.app.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.healtheat.app.BuildConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/** In-memory cookies: no JWT in localStorage/SharedPreferences or logs. Logout/restart clears them. */
class SessionCookieJar : CookieJar {
    private val jar = mutableListOf<Cookie>()
    @Synchronized override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
            jar.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
            if (cookie.expiresAt > System.currentTimeMillis()) jar.add(cookie)
        }
    }
    @Synchronized override fun loadForRequest(url: HttpUrl): List<Cookie> {
        jar.removeAll { it.expiresAt <= System.currentTimeMillis() }
        return jar.filter { it.matches(url) }
    }
    @Synchronized fun clear() = jar.clear()
}

interface HealtheatApi {
    @GET("api/auth/csrf") suspend fun csrf(): Response<JsonElement>
    @POST("api/auth/signup") suspend fun signup(@Body body: JsonObject): Response<JsonElement>
    @POST("api/auth/login") suspend fun login(@Body body: JsonObject): Response<JsonElement>
    @POST("api/auth/logout") suspend fun logout(): Response<JsonElement>
    @GET("api/users/me") suspend fun me(): Response<JsonElement>
    @GET("api/users/me/profile") suspend fun basicProfile(): Response<JsonElement>
    @PUT("api/users/me/profile") suspend fun saveBasicProfile(@Body body: JsonObject): Response<JsonElement>
    @GET("api/users/me/preferences") suspend fun preferences(): Response<JsonElement>
    @PATCH("api/users/me/preferences") suspend fun savePreferences(@Body body: JsonObject): Response<JsonElement>
    @POST("api/health-profiles/analyze") suspend fun analyze(@Body body: JsonObject): Response<JsonElement>
    @GET("api/health-profiles") suspend fun healthProfile(): Response<JsonElement>
    @PUT("api/health-profiles") suspend fun saveHealth(@Body body: JsonObject): Response<JsonElement>
    @GET("api/meal-plans/today") suspend fun today(): Response<JsonElement>
    @POST("api/meal-plans/generate") suspend fun generate(): Response<JsonElement>
    @POST("api/meal-plans/{planId}/meals/{mealType}/regenerate")
    suspend fun regenerate(@Path("planId") planId: Long, @Path("mealType") mealType: String, @Body body: JsonObject): Response<JsonElement>
    @GET("api/recipes/{recipeId}") suspend fun recipe(@Path("recipeId") recipeId: Long): Response<JsonElement>
    @GET("api/meal-records") suspend fun records(@Query("from") from: String, @Query("to") to: String): Response<JsonElement>
    @POST("api/meal-records") suspend fun record(@Body body: JsonObject): Response<JsonElement>
    @POST("api/meal-records/from-recommendation") suspend fun recordRecommended(@Body body: JsonObject): Response<JsonElement>
    @POST("api/chat/sessions") suspend fun startChat(): Response<JsonElement>
    @GET("api/chat/sessions") suspend fun chatSessions(): Response<JsonElement>
    @GET("api/chat/sessions/{sessionId}/messages") suspend fun chatHistory(@Path("sessionId") sessionId: Long): Response<JsonElement>
    @POST("api/chat/messages") suspend fun send(@Body body: JsonObject): Response<JsonElement>
    @DELETE("api/chat/sessions/{sessionId}") suspend fun endChat(@Path("sessionId") sessionId: Long): Response<JsonElement>
}

class ApiException(val statusCode: Int, val errorCode: String?, override val message: String) : Exception(message)

object ApiClient {
    /** The supplied backend always issues Secure cookies. HTTPS is required, even for emulator testing. */
    fun requireHttpsForLogin() {
        if (!BuildConfig.API_BASE_URL.startsWith("https://", ignoreCase = true)) {
            throw IllegalStateException(
                "백엔드가 Secure 인증 쿠키를 발급하므로 HTTP 주소로 로그인할 수 없어요. " +
                    "Android의 healtheatApiUrl을 접속 가능한 HTTPS API 주소로 설정해 주세요."
            )
        }
    }
    val cookies = SessionCookieJar()
    @Volatile private var currentCsrfToken: String? = null
    @Volatile private var currentCsrfHeader: String = "X-XSRF-TOKEN"
    private val csrfHeader = Interceptor { chain ->
        val original = chain.request()
        val request = original.newBuilder()
        // Spring Security sends a masked CSRF token in GET /api/auth/csrf JSON.
        // The readable XSRF-TOKEN cookie is not necessarily the value expected in the header.
        if (original.method !in setOf("GET", "HEAD", "OPTIONS")) {
            currentCsrfToken?.let { request.header(currentCsrfHeader, it) }
        }
        chain.proceed(request.build())
    }
    private val http = OkHttpClient.Builder()
        .cookieJar(cookies)
        .addInterceptor(csrfHeader)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // LLM may take longer than ordinary CRUD.
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()
    val service: HealtheatApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(http).build().create(HealtheatApi::class.java)

    /** Must run before every POST/PUT/PATCH/DELETE. Fetch again after login. */
    suspend fun prepareMutation() {
        val response = service.csrf().requireBodyOrNull()
        val json = response?.takeIf { it.isJsonObject }?.asJsonObject
            ?: throw IllegalStateException("CSRF 응답이 올바르지 않아요")
        val token = json.get("token")?.takeIf { it.isJsonPrimitive }?.asString
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("서버에서 CSRF 토큰을 받지 못했어요")
        val header = json.get("headerName")?.takeIf { it.isJsonPrimitive }?.asString
            ?: "X-XSRF-TOKEN"
        currentCsrfHeader = header
        currentCsrfToken = token
    }

    fun clearSession() {
        cookies.clear()
        currentCsrfToken = null
    }
}

fun Response<JsonElement>.requireBodyOrNull(): JsonElement? {
    if (!isSuccessful) {
        val content = try { errorBody()?.string() } catch (_: Exception) { null }
        val parsed = try { com.google.gson.JsonParser.parseString(content).asJsonObject } catch (_: Exception) { null }
        val errorCode = parsed?.get("code")?.takeIf { it.isJsonPrimitive }?.asString
        val detail = parsed?.get("message")?.takeIf { it.isJsonPrimitive }?.asString
        throw ApiException(code(), errorCode, detail ?: when(code()) {
            400 -> "입력 내용을 확인해 주세요 (400)"
            401 -> "로그인이 만료되었어요. 다시 로그인해 주세요 (401)"
            403 -> "요청 권한 또는 CSRF 토큰을 확인해 주세요 (403)"
            404 -> "요청한 정보를 찾지 못했어요 (404)"
            409 -> "이미 저장된 기록이 있거나 변경할 수 없는 상태예요 (409)"
            410 -> "채팅 세션이 만료되었어요 (410)"
            else -> "서버 요청 실패 (HTTP ${code()})"
        })
    }
    return body()
}

fun obj(vararg fields: Pair<String, Any?>): JsonObject = JsonObject().also { json ->
    for ((key, value) in fields) {
        when(value) {
            null -> json.add(key, com.google.gson.JsonNull.INSTANCE)
            is String -> json.addProperty(key, value)
            is Number -> json.addProperty(key, value)
            is Boolean -> json.addProperty(key, value)
            is JsonElement -> json.add(key, value)
            else -> throw IllegalArgumentException("Unsupported field type for $key")
        }
    }
}
