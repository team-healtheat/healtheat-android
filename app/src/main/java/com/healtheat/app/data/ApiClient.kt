
package com.healtheat.app.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.healtheat.app.BuildConfig
import okhttp3.Authenticator
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response as OkHttpResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// 인증 쿠키를 메모리와 Android Keystore 기반 암호화 저장소에서 관리
class SessionCookieJar : CookieJar {

    private val jar = mutableListOf<Cookie>()
    private var prefs: SharedPreferences? = null

    private val alias = "healtheat_session_key"
    private val cookieNames = setOf(
        "access_token",
        "refresh_token"
    )

    @Synchronized
    fun hasRestorableSession(): Boolean {
        return jar.any {
            it.name in cookieNames &&
                    it.expiresAt > System.currentTimeMillis()
        }
    }

    @Synchronized
    fun initialize(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(
            "healtheat_secure_session",
            Context.MODE_PRIVATE
        )

        val encrypted =
            prefs?.getString("cookies", null) ?: return

        try {
            val url = BuildConfig.API_BASE_URL.toHttpUrl()
            val saved = decrypt(encrypted)

            jar.clear()

            saved.lineSequence()
                .mapNotNull { Cookie.parse(url, it) }
                .filter {
                    it.name in cookieNames &&
                            it.expiresAt > System.currentTimeMillis() &&
                            it.matches(
                                if (it.name == "refresh_token") {
                                    url.resolve("api/auth/refresh") ?: url
                                } else {
                                    url
                                }
                            )
                }
                .forEach { jar.add(it) }

        } catch (_: Exception) {
            jar.clear()
            prefs?.edit()
                ?.remove("cookies")
                ?.commit()
        }
    }

    @Synchronized
    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>
    ) {
        cookies.forEach { cookie ->
            jar.removeAll {
                it.name == cookie.name &&
                        it.domain == cookie.domain &&
                        it.path == cookie.path
            }

            if (cookie.expiresAt > System.currentTimeMillis()) {
                jar.add(cookie)
            }
        }

        persist()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        jar.removeAll {
            it.expiresAt <= System.currentTimeMillis()
        }

        return jar.filter { it.matches(url) }
    }

    @Synchronized
    fun clear() {
        jar.clear()
        prefs?.edit()
            ?.remove("cookies")
            ?.commit()
    }

    private fun persist() {
        val value = jar
            .filter {
                it.name in cookieNames &&
                        it.expiresAt > System.currentTimeMillis()
            }
            .joinToString("\n") {
                it.toString()
            }

        if (value.isBlank()) {
            prefs?.edit()
                ?.remove("cookies")
                ?.commit()
            return
        }

        val encrypted = encrypt(value)

        prefs?.edit()
            ?.putString("cookies", encrypted)
            ?.commit()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore
            .getInstance("AndroidKeyStore")
            .apply { load(null) }

        val existing =
            keyStore.getKey(alias, null) as? SecretKey

        if (existing != null) {
            return existing
        }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(
                KeyProperties.BLOCK_MODE_GCM
            )
            .setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_NONE
            )
            .build()

        generator.init(spec)
        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(
            "AES/GCM/NoPadding"
        )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey()
        )

        val encrypted = cipher.doFinal(
            value.toByteArray(Charsets.UTF_8)
        )

        return Base64.encodeToString(
            cipher.iv + encrypted,
            Base64.NO_WRAP
        )
    }

    private fun decrypt(value: String): String {
        val bytes = Base64.decode(
            value,
            Base64.NO_WRAP
        )

        require(bytes.size > 12)

        val iv = bytes.copyOfRange(0, 12)
        val data = bytes.copyOfRange(
            12,
            bytes.size
        )

        val cipher = Cipher.getInstance(
            "AES/GCM/NoPadding"
        )

        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(128, iv)
        )

        return String(
            cipher.doFinal(data),
            Charsets.UTF_8
        )
    }
}

interface HealtheatApi {

    @GET("api/auth/csrf")
    suspend fun csrf(): Response<JsonElement>

    @POST("api/auth/signup")
    suspend fun signup(
        @Body body: JsonObject
    ): Response<JsonElement>

    @POST("api/auth/login")
    suspend fun login(
        @Body body: JsonObject
    ): Response<JsonElement>

    @POST("api/auth/logout")
    suspend fun logout(): Response<JsonElement>

    @POST("api/auth/refresh")
    suspend fun refreshToken(): Response<JsonElement>

    @GET("api/users/me")
    suspend fun me(): Response<JsonElement>

    @GET("api/users/me/profile")
    suspend fun basicProfile(): Response<JsonElement>

    @PUT("api/users/me/profile")
    suspend fun saveBasicProfile(
        @Body body: JsonObject
    ): Response<JsonElement>

    @GET("api/users/me/preferences")
    suspend fun preferences(): Response<JsonElement>

    @PATCH("api/users/me/preferences")
    suspend fun savePreferences(
        @Body body: JsonObject
    ): Response<JsonElement>

    @PATCH("api/users/me/nickname")
    suspend fun updateNickname(
        @Body body: JsonObject
    ): Response<JsonElement>

    @HTTP(
        method = "DELETE",
        path = "api/users/me",
        hasBody = true
    )
    suspend fun deleteAccount(
        @Body body: JsonObject
    ): Response<JsonElement>

    @POST("api/health-profiles/analyze")
    suspend fun analyze(
        @Body body: JsonObject
    ): Response<JsonElement>

    @GET("api/health-profiles")
    suspend fun healthProfile(): Response<JsonElement>

    @PUT("api/health-profiles")
    suspend fun saveHealth(
        @Body body: JsonObject
    ): Response<JsonElement>

    @GET("api/meal-plans/today")
    suspend fun today(): Response<JsonElement>

    @POST("api/meal-plans/generate")
    suspend fun generate(): Response<JsonElement>

    @POST("api/meal-plans/{planId}/meals/{mealType}/regenerate")
    suspend fun regenerate(
        @Path("planId") planId: Long,
        @Path("mealType") mealType: String,
        @Body body: JsonObject
    ): Response<JsonElement>

    @GET("api/recipes/{recipeId}")
    suspend fun recipe(
        @Path("recipeId") recipeId: Long
    ): Response<JsonElement>

    @GET("api/meal-records")
    suspend fun records(
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<JsonElement>

    @POST("api/meal-records")
    suspend fun record(
        @Body body: JsonObject
    ): Response<JsonElement>

    @POST("api/meal-records/from-recommendation")
    suspend fun recordRecommended(
        @Body body: JsonObject
    ): Response<JsonElement>

    @PUT("api/meal-records/{recordId}")
    suspend fun updateMealRecord(
        @Path("recordId") recordId: Long,
        @Body body: JsonObject
    ): Response<JsonElement>

    @POST("api/chat/sessions")
    suspend fun startChat(): Response<JsonElement>

    @GET("api/chat/sessions")
    suspend fun chatSessions(): Response<JsonElement>

    @GET("api/chat/sessions/{sessionId}/messages")
    suspend fun chatHistory(
        @Path("sessionId") sessionId: Long
    ): Response<JsonElement>

    @POST("api/chat/messages")
    suspend fun send(
        @Body body: JsonObject
    ): Response<JsonElement>

    @DELETE("api/chat/sessions/{sessionId}")
    suspend fun endChat(
        @Path("sessionId") sessionId: Long
    ): Response<JsonElement>
}

class ApiException(
    val statusCode: Int,
    val errorCode: String?,
    override val message: String
) : Exception(message)

object ApiClient {

    val cookies = SessionCookieJar()

    @Volatile
    private var currentCsrfToken: String? = null

    @Volatile
    private var currentCsrfHeader: String =
        "X-XSRF-TOKEN"

    // 여러 API가 동시에 401을 받아도 재발급 요청을 직렬화
    private val refreshLock = Any()

    fun initialize(context: Context) {
        cookies.initialize(context)
    }

    fun requireHttpsForLogin() {
        if (
            !BuildConfig.API_BASE_URL.startsWith(
                "https://",
                ignoreCase = true
            )
        ) {
            throw IllegalStateException(
                "인증 쿠키 사용을 위해 HTTPS API 주소가 필요해요."
            )
        }
    }

    private val csrfHeader = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()

        if (
            original.method !in setOf(
                "GET",
                "HEAD",
                "OPTIONS"
            )
        ) {
            currentCsrfToken?.let { token ->
                builder.header(
                    currentCsrfHeader,
                    token
                )
            }
        }

        chain.proceed(builder.build())
    }

    // 토큰 재발급 전용 클라이언트:
    // Authenticator가 없어 재귀적으로 401을 처리하지 않음
    private val refreshHttp = OkHttpClient.Builder()
        .cookieJar(cookies)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun isRefreshExcluded(
        response: OkHttpResponse
    ): Boolean {
        val path = response.request.url.encodedPath

        // 로그인 실패, 회원가입 오류, 로그아웃은 자동 재발급 제외
        if (path.contains("/api/auth/")) {
            return true
        }

        // 회원탈퇴 시 틀린 비밀번호로 반환된 401을
        // 토큰 만료로 오해하지 않도록 제외
        if (
            response.request.method == "DELETE" &&
            path.endsWith("/api/users/me")
        ) {
            return true
        }

        // 동일 요청에 대한 무한 재시도 방지
        var previous = response.priorResponse

        while (previous != null) {
            if (previous.code == 401) {
                return true
            }
            previous = previous.priorResponse
        }

        return false
    }

    private fun requestAccessToken(
        response: OkHttpResponse
    ): String? {
        return response.request.header("Cookie")
            ?.split(";")
            ?.map { it.trim() }
            ?.firstOrNull {
                it.startsWith("access_token=")
            }
            ?.substringAfter("=")
    }

    private fun renewAccessToken(): Boolean {
        return try {
            val baseUrl =
                BuildConfig.API_BASE_URL.toHttpUrl()

            val csrfUrl =
                baseUrl.resolve("api/auth/csrf")
                    ?: return false

            val refreshUrl =
                baseUrl.resolve("api/auth/refresh")
                    ?: return false

            // Refresh Token이 없으면 갱신 불가
            val hasRefreshToken = cookies
                .loadForRequest(refreshUrl)
                .any {
                    it.name == "refresh_token"
                }

            if (!hasRefreshToken) {
                return false
            }

            // 새 CSRF 토큰 요청
            val csrfRequest = Request.Builder()
                .url(csrfUrl)
                .get()
                .build()

            val csrfResponse = refreshHttp
                .newCall(csrfRequest)
                .execute()

            val csrfResult = csrfResponse.use { result ->
                if (!result.isSuccessful) {
                    return false
                }

                val json = JsonParser.parseString(
                    result.body?.string()
                        ?: return false
                ).asJsonObject

                val token = json
                    .get("token")
                    ?.asString
                    ?.takeIf { it.isNotBlank() }
                    ?: return false

                val header = json
                    .get("headerName")
                    ?.asString
                    ?.takeIf { it.isNotBlank() }
                    ?: "X-XSRF-TOKEN"

                header to token
            }

            val (headerName, csrfToken) = csrfResult

            currentCsrfHeader = headerName
            currentCsrfToken = csrfToken

            // Refresh Token 회전 및 Access Token 재발급
            val refreshRequest = Request.Builder()
                .url(refreshUrl)
                .header(
                    headerName,
                    csrfToken
                )
                .post(
                    ByteArray(0).toRequestBody()
                )
                .build()

            val refreshResponse = refreshHttp
                .newCall(refreshRequest)
                .execute()

            refreshResponse.use {
                it.isSuccessful &&
                        cookies.loadForRequest(baseUrl)
                            .any { cookie ->
                                cookie.name == "access_token"
                            }
            }
        } catch (_: Exception) {
            // 재발급 실패를 로그에 토큰과 함께 노출하지 않음
            false
        }
    }

    private val automaticRefresh = Authenticator {
            _,
            response ->

        if (isRefreshExcluded(response)) {
            null
        } else {
            synchronized(refreshLock) {

                val url = response.request.url
                val oldAccess =
                    requestAccessToken(response)

                val activeAccess = cookies
                    .loadForRequest(url)
                    .firstOrNull {
                        it.name == "access_token"
                    }
                    ?.value

                // 다른 요청이 먼저 갱신했다면
                // 추가 재발급 없이 새 쿠키로 재시도
                val alreadyRefreshed =
                    activeAccess != null &&
                            activeAccess != oldAccess

                val renewed = if (alreadyRefreshed) {
                    true
                } else {
                    renewAccessToken()
                }

                if (!renewed) {
                    null
                } else {
                    val latestCookies = cookies
                        .loadForRequest(url)

                    val hasAccess = latestCookies.any {
                        it.name == "access_token"
                    }

                    if (!hasAccess) {
                        null
                    } else {
                        val cookieValue = latestCookies
                            .joinToString("; ") {
                                "${it.name}=${it.value}"
                            }

                        val builder = response.request
                            .newBuilder()
                            .header(
                                "Cookie",
                                cookieValue
                            )

                        // 재발급 중 CSRF 쿠키가 바뀔 수 있으므로
                        // 원래 POST/PUT/PATCH/DELETE의 헤더도 갱신
                        if (
                            response.request.method !in setOf(
                                "GET",
                                "HEAD",
                                "OPTIONS"
                            )
                        ) {
                            currentCsrfToken?.let { token ->
                                builder.header(
                                    currentCsrfHeader,
                                    token
                                )
                            }
                        }

                        builder.build()
                    }
                }
            }
        }
    }

    private val http = OkHttpClient.Builder()
        .cookieJar(cookies)
        .addInterceptor(csrfHeader)
        .authenticator(automaticRefresh)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    val service: HealtheatApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .addConverterFactory(
            GsonConverterFactory.create()
        )
        .client(http)
        .build()
        .create(HealtheatApi::class.java)

    // 모든 POST/PUT/PATCH/DELETE 전에 호출
    suspend fun prepareMutation() {
        val response = service.csrf()
            .requireBodyOrNull()

        val json = response
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
            ?: throw IllegalStateException(
                "CSRF 응답이 올바르지 않아요"
            )

        val token = json
            .get("token")
            ?.takeIf { it.isJsonPrimitive }
            ?.asString
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "서버에서 CSRF 토큰을 받지 못했어요"
            )

        val header = json
            .get("headerName")
            ?.takeIf { it.isJsonPrimitive }
            ?.asString
            ?: "X-XSRF-TOKEN"

        currentCsrfHeader = header
        currentCsrfToken = token
    }

    fun clearSession() {
        synchronized(refreshLock) {
            cookies.clear()
            currentCsrfToken = null
            currentCsrfHeader = "X-XSRF-TOKEN"
        }
    }
}

fun Response<JsonElement>.requireBodyOrNull(): JsonElement? {
    if (!isSuccessful) {
        val content = try {
            errorBody()?.string()
        } catch (_: Exception) {
            null
        }

        val parsed = try {
            JsonParser.parseString(content)
                .asJsonObject
        } catch (_: Exception) {
            null
        }

        val errorCode = parsed
            ?.get("code")
            ?.takeIf { it.isJsonPrimitive }
            ?.asString

        val detail = parsed
            ?.get("message")
            ?.takeIf { it.isJsonPrimitive }
            ?.asString

        throw ApiException(
            code(),
            errorCode,
            detail ?: when (code()) {
                400 -> "입력 내용을 확인해 주세요 (400)"
                401 -> "로그인이 만료되었어요. 다시 로그인해 주세요 (401)"
                403 -> "요청 권한 또는 CSRF 토큰을 확인해 주세요 (403)"
                404 -> "요청한 정보를 찾지 못했어요 (404)"
                409 -> "이미 저장된 기록이 있거나 변경할 수 없는 상태예요 (409)"
                410 -> "채팅 세션이 만료되었어요 (410)"
                else -> "서버 요청 실패 (HTTP ${code()})"
            }
        )
    }

    return body()
}

fun obj(
    vararg fields: Pair<String, Any?>
): JsonObject = JsonObject().also { json ->
    for ((key, value) in fields) {
        when (value) {
            null -> json.add(
                key,
                com.google.gson.JsonNull.INSTANCE
            )

            is String ->
                json.addProperty(key, value)

            is Number ->
                json.addProperty(key, value)

            is Boolean ->
                json.addProperty(key, value)

            is JsonElement ->
                json.add(key, value)

            else ->
                throw IllegalArgumentException(
                    "Unsupported field type for $key"
                )
        }
    }
}
