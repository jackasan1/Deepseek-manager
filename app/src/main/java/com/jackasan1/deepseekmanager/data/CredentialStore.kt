package com.jackasan1.deepseekmanager.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * The API key is the only secret this app holds, and it is the only
 * thing that should ever be able to spend the user's money — so it lives
 * behind a hardware-backed Keystore master key, never in plain prefs,
 * never in logs, never in a backup (allowBackup=false in the manifest).
 *
 * NOTE: the user's *account password* is never requested, never stored,
 * and never transmitted by this app.
 */
class CredentialStore(context: Context) {

    private val prefs by lazy {
        @Suppress("DEPRECATION")
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        @Suppress("DEPRECATION")
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun apiKey(): String? = prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }

    fun saveApiKey(value: String) {
        prefs.edit().putString(KEY_API_KEY, value.trim()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_API_KEY).apply()
    }

    /** Only ever used to render "sk-abc…xyz" — the full key is never shown back. */
    fun maskedKey(): String? = apiKey()?.let { key ->
        if (key.length <= 10) "sk-••••" else "${key.take(6)}…${key.takeLast(4)}"
    }

    private companion object {
        const val PREFS_NAME = "deepseek_secure_prefs"
        const val KEY_API_KEY = "api_key"
    }
}
