package com.healtheat.app.ui.screens

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class OnboardingDraft(
    val version: Int = 2,
    val timestamp: Long = System.currentTimeMillis(),
    val step: Int = 1,
    val age: String = "",
    val gender: String = "선택 안 함",
    val diningOut: Boolean = true,
    val selectedDiagnoses: List<String> = emptyList(),
    val otherDiagnosis: String = "",
    val noDiagnosis: Boolean = false,
    val allergyStatus: String = "미응답",
    val allergies: List<String> = emptyList(),
    val otherAllergy: String = "",
    val liked: String = "",
    val disliked: String = "",
    val style: String = ""
)

/** Keystore AES-GCM 임시 작성 내용. 서버 전송 없음. */
class OnboardingDraftStore(context: Context) {
    companion object {
        const val EXPIRATION_MILLIS = 30 * 60 * 1000L
        private const val ALIAS = "healtheat_onboarding_draft_v2"
        private const val NAME = "healtheat_onboarding_secure_draft"
    }
    private val prefs = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        // 과거 AI 건강요인 분석 입력값을 저장하던 v1 임시 데이터를 정리한다.
        context.applicationContext.getSharedPreferences(
            "healtheat_onboarding_draft_v1", Context.MODE_PRIVATE
        ).edit().clear().apply()
    }

    private fun key(account: String, initial: Boolean): String {
        val value = (account.trim().lowercase() + if (initial) "_init" else "_edit")
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    @Synchronized
    fun load(account: String, initial: Boolean): OnboardingDraft? {
        if (account.isBlank()) return null
        val k = key(account, initial)
        val encoded = prefs.getString(k, null) ?: return null
        return try {
            val draft = gson.fromJson(decrypt(encoded), OnboardingDraft::class.java)
            val ageMillis = System.currentTimeMillis() - draft.timestamp
            if (draft.version != 2 || draft.step !in 1..5 ||
                ageMillis < 0 || ageMillis >= EXPIRATION_MILLIS
            ) {
                clear(account, initial)
                null
            } else draft
        } catch (_: Exception) {
            clear(account, initial)
            null
        }
    }

    @Synchronized
    fun save(account: String, initial: Boolean, draft: OnboardingDraft) {
        if (account.isBlank()) return
        try {
            prefs.edit().putString(
                key(account, initial),
                encrypt(gson.toJson(draft.copy(timestamp = System.currentTimeMillis())))
            ).commit()
        } catch (_: Exception) {
            // 저장 실패는 작성 화면 이용을 막지 않는다.
        }
    }

    @Synchronized
    fun clear(account: String, initial: Boolean) {
        if (account.isNotBlank()) prefs.edit().remove(key(account, initial)).commit()
    }

    private fun secret(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()
        )
        return generator.generateKey()
    }

    private fun encrypt(source: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secret())
        return Base64.encodeToString(
            cipher.iv + cipher.doFinal(source.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP
        )
    }

    private fun decrypt(source: String): String {
        val bytes = Base64.decode(source, Base64.NO_WRAP)
        require(bytes.size > 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            secret(),
            GCMParameterSpec(128, bytes.copyOfRange(0, 12))
        )
        return String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    }
}
