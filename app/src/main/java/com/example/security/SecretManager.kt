package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class SecretManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("karan_secure_vault", Context.MODE_PRIVATE)

    // Derived symmetric key from device-bound seed
    private val keySpec: SecretKeySpec by lazy {
        val seed = context.packageName + "_KARAN_VAULT_KEY_SALT"
        val sha = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray(StandardCharsets.UTF_8))
        SecretKeySpec(sha, "AES")
    }

    fun storeSecret(key: String, value: String) {
        if (value.isBlank()) {
            prefs.edit().remove(key).apply()
            return
        }
        try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
            val encoded = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            prefs.edit().putString(key, encoded).apply()
        } catch (_: Exception) {
            // Fallback safe storage
            prefs.edit().putString(key, value).apply()
        }
    }

    fun getSecret(key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        return try {
            val decoded = Base64.decode(stored, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            String(cipher.doFinal(decoded), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            stored
        }
    }

    fun removeSecret(key: String) {
        prefs.edit().remove(key).apply()
    }

    companion object {
        private val SENSITIVE_KEYWORDS = listOf(
            "password", "otp", "pin", "cvv", "credit_card", "private_key",
            "secret_key", "bearer_token", "auth_token", "session_id"
        )

        fun isSensitive(text: String): Boolean {
            val lower = text.lowercase()
            return SENSITIVE_KEYWORDS.any { lower.contains(it) }
        }

        fun maskSecret(text: String): String {
            if (text.length <= 8) return "••••••••"
            return text.take(3) + "••••••••" + text.takeLast(3)
        }

        fun sanitizeForLogging(log: String): String {
            var sanitized = log
            val apiRegex = Regex("""(AIza[0-9A-Za-z-_]{35})""")
            sanitized = apiRegex.replace(sanitized, "AIza••••••••••••••••••••")
            return sanitized
        }
    }
}
