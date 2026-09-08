package com.example.assistant.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Manages encrypted API Keys (Gemini, Weather, News, etc.) for MAYA.
 * Keys are encrypted using AndroidKeyStore hardware-backed AES/GCM
 * and persisted in private SharedPreferences.
 */
class ApiKeyManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "ApiKeyManager"
        private const val PREFS_NAME = "maya_api_keys"
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_WEATHER = "weather_api_key"
        private const val KEY_NEWS = "news_api_key"

        @Volatile
        private var instance: ApiKeyManager? = null

        fun getInstance(context: Context): ApiKeyManager {
            return instance ?: synchronized(this) {
                instance ?: ApiKeyManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val encryptionManager: EncryptionManager by lazy {
        EncryptionManager.getInstance(context)
    }

    fun saveApiKey(serviceName: String, apiKey: String): Boolean {
        return try {
            val trimmed = apiKey.trim()
            if (trimmed.isEmpty()) {
                return deleteApiKey(serviceName)
            }
            val encryptedKey = encryptionManager.encrypt(trimmed)
            if (encryptedKey.isEmpty()) {
                Log.e(TAG, "Failed to encrypt API key for $serviceName")
                return false
            }
            val prefKey = getPreferenceKey(serviceName)
            prefs.edit().putString(prefKey, encryptedKey).apply()
            Log.d(TAG, "Saved encrypted API key for service: $serviceName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving API key for $serviceName: ${e.message}", e)
            false
        }
    }

    fun getApiKey(serviceName: String): String? {
        return try {
            val prefKey = getPreferenceKey(serviceName)
            val encryptedKey = prefs.getString(prefKey, null) ?: return null
            val decrypted = encryptionManager.decrypt(encryptedKey)
            if (decrypted.isNotEmpty()) decrypted else null
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting API key for $serviceName: ${e.message}", e)
            null
        }
    }

    fun deleteApiKey(serviceName: String): Boolean {
        return try {
            val prefKey = getPreferenceKey(serviceName)
            prefs.edit().remove(prefKey).apply()
            Log.d(TAG, "Deleted API key for service: $serviceName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting API key for $serviceName: ${e.message}", e)
            false
        }
    }

    fun getAllApiKeys(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val services = listOf("gemini", "weather", "news")
        services.forEach { service ->
            getApiKey(service)?.let { key ->
                result[service] = key
            }
        }
        return result
    }

    fun hasApiKey(serviceName: String): Boolean {
        val prefKey = getPreferenceKey(serviceName)
        return prefs.contains(prefKey) && !getApiKey(serviceName).isNullOrBlank()
    }

    private fun getPreferenceKey(serviceName: String): String {
        return when (serviceName.trim().lowercase()) {
            "gemini" -> KEY_GEMINI
            "weather" -> KEY_WEATHER
            "news" -> KEY_NEWS
            else -> "api_key_${serviceName.trim().lowercase()}"
        }
    }
}
