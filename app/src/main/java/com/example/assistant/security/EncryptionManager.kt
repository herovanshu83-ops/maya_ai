package com.example.assistant.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptionManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "EncryptionManager"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "maya_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        @Volatile
        private var instance: EncryptionManager? = null

        fun getInstance(context: Context): EncryptionManager {
            return instance ?: synchronized(this) {
                instance ?: EncryptionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val masterKey: SecretKey by lazy { getOrCreateMasterKey() }

    private fun getOrCreateMasterKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
            keyStore.load(null)

            if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                val keyEntry = keyStore.getEntry(MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                if (keyEntry != null) {
                    return keyEntry.secretKey
                }
            }

            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AndroidKeyStore master key, falling back", e)
            // Software fallback key for environments where KeyStore has restrictions
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            keyGenerator.generateKey()
        }
    }

    fun encrypt(data: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, masterKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))

            // Combine IV + encrypted data
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            ""
        }
    }

    fun decrypt(encryptedData: String): String {
        return try {
            val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
            if (combined.size <= 12) return ""
            val iv = combined.copyOfRange(0, 12) // AES-GCM standard IV is 12 bytes
            val encryptedBytes = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(128, iv))
            val decryptedBytes = cipher.doFinal(encryptedBytes)

            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            ""
        }
    }

    fun encryptApiKey(apiKey: String): String = encrypt(apiKey)
    fun decryptApiKey(encryptedKey: String): String = decrypt(encryptedKey)
}
