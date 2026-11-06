
package com.healtheat.app.ui.screens

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import com.healtheat.app.data.HealthFactor
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// 개인정보 설정 중인 내용만 임시 저장
data class OnboardingDraft(
    val schemaVersion: Int = 1,
    val updatedAtMillis: Long = 0L,
    val step: Int = 1,
    val age: String = "",
    val gender: String = "선택 안 함",
    val diningOut: Boolean = true,
    val healthText: String = "",
    val analyzed: Boolean = false,
    val confirmed: Boolean = false,
    val factors: List<HealthFactor> = emptyList(),
    val allergyStatus: String = "미응답",
    val allergies: List<String> = emptyList(),
    val otherAllergy: String = "",
    val liked: String = "",
    val disliked: String = "",
    val style: String = ""
)

class OnboardingDraftStore(
    context: Context
) {
    companion object {
        const val EXPIRATION_MILLIS = 30 * 60 * 1000L

        private const val PREF_NAME =
            "healtheat_onboarding_draft_v1"

        private const val KEY_ALIAS =
            "healtheat_onboarding_draft_aes_v1"
    }

    private val prefs =
        context.applicationContext.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )

    private val gson = Gson()

    // 계정 및 최초 설정/수정 모드별로 저장 공간 분리
    private fun entryKey(
        account: String,
        isInitialSetup: Boolean
    ): String {
        val raw = (
                account.trim().lowercase() +
                        "|" +
                        if (isInitialSetup) "initial" else "edit"
                ).toByteArray(Charsets.UTF_8)

        return MessageDigest
            .getInstance("SHA-256")
            .digest(raw)
            .joinToString("") {
                "%02x".format(it.toInt() and 0xff)
            }
    }

    @Synchronized
    fun load(
        account: String,
        isInitialSetup: Boolean
    ): OnboardingDraft? {
        if (account.isBlank()) return null

        val key = entryKey(account, isInitialSetup)
        val encrypted = prefs.getString(key, null)
            ?: return null

        return try {
            val json = decrypt(encrypted)

            val draft = gson.fromJson(
                json,
                OnboardingDraft::class.java
            ) ?: throw IllegalStateException(
                "임시 저장 데이터가 비어 있어요."
            )

            val elapsed =
                System.currentTimeMillis() -
                        draft.updatedAtMillis

            if (
                draft.schemaVersion != 1 ||
                draft.step !in 1..5 ||
                elapsed < 0 ||
                elapsed >= EXPIRATION_MILLIS
            ) {
                prefs.edit().remove(key).commit()
                null
            } else {
                draft
            }
        } catch (_: Exception) {
            // 저장값이 손상되었거나 암호화 키를 읽을 수 없으면 초기화
            prefs.edit().remove(key).commit()
            null
        }
    }

    @Synchronized
    fun save(
        account: String,
        isInitialSetup: Boolean,
        draft: OnboardingDraft
    ) {
        if (account.isBlank()) return

        try {
            val key = entryKey(
                account,
                isInitialSetup
            )

            val current = draft.copy(
                schemaVersion = 1,
                updatedAtMillis =
                    System.currentTimeMillis()
            )

            val encrypted = encrypt(
                gson.toJson(current)
            )

            prefs.edit()
                .putString(key, encrypted)
                .commit()
        } catch (_: Exception) {
            // 암호화 저장에 실패해도 화면 입력은 계속 가능
        }
    }

    @Synchronized
    fun clear(
        account: String,
        isInitialSetup: Boolean
    ) {
        if (account.isBlank()) return

        prefs.edit()
            .remove(
                entryKey(account, isInitialSetup)
            )
            .commit()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(
            "AndroidKeyStore"
        ).apply {
            load(null)
        }

        val existing = keyStore.getKey(
            KEY_ALIAS,
            null
        ) as? SecretKey

        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
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

    private fun encrypt(text: String): String {
        val cipher = Cipher.getInstance(
            "AES/GCM/NoPadding"
        )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey()
        )

        val encrypted = cipher.doFinal(
            text.toByteArray(Charsets.UTF_8)
        )

        return Base64.encodeToString(
            cipher.iv + encrypted,
            Base64.NO_WRAP
        )
    }

    private fun decrypt(encoded: String): String {
        val bytes = Base64.decode(
            encoded,
            Base64.NO_WRAP
        )

        require(bytes.size > 12)

        val iv = bytes.copyOfRange(0, 12)
        val encrypted = bytes.copyOfRange(
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
            cipher.doFinal(encrypted),
            Charsets.UTF_8
        )
    }
}
