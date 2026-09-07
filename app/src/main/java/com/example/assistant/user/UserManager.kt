package com.example.assistant.user

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.assistant.security.EncryptionManager
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

sealed class SignupResult {
    data class Success(val user: UserEntity) : SignupResult()
    data class Failure(val message: String) : SignupResult()
}

sealed class LoginResult {
    data class Success(val user: UserEntity) : LoginResult()
    data class Failure(val message: String) : LoginResult()
}

class UserManager private constructor(
    private val context: Context,
    private val database: AppDatabase,
    private val encryptionManager: EncryptionManager
) {

    companion object {
        private const val TAG = "UserManager"
        private const val PREFS_NAME = "maya_user_prefs"
        private const val PREF_CURRENT_USER = "current_user_id"
        private const val PREF_SESSION_TOKEN = "session_token"
        private const val PREF_GUEST_MODE = "guest_mode"

        @Volatile
        private var instance: UserManager? = null

        fun getInstance(context: Context): UserManager {
            return instance ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val enc = EncryptionManager.getInstance(context)
                instance ?: UserManager(context.applicationContext, db, enc).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val userDao = database.userDao()
    private val sessionDao = database.sessionDao()
    private val apiKeyDao = database.apiKeyDao()
    private val welcomeMessageDao = database.welcomeMessageDao()
    private val activityDao = database.userActivityDao()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isGuestMode = MutableStateFlow(false)
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            loadCurrentUser()
        }
    }

    private suspend fun loadCurrentUser() {
        val userId = prefs.getString(PREF_CURRENT_USER, null)
        val isGuest = prefs.getBoolean(PREF_GUEST_MODE, false)
        _isGuestMode.value = isGuest

        if (userId != null) {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val session = sessionDao.getActiveSession(userId)
                if (session == null || session.expiresAt < System.currentTimeMillis()) {
                    logout()
                } else {
                    _currentUser.value = user
                }
            }
        }
    }

    suspend fun signup(
        email: String,
        password: String,
        displayName: String,
        profilePicture: String? = null
    ): SignupResult {
        if (email.isBlank() || !email.contains("@")) {
            return SignupResult.Failure("Please enter a valid email address")
        }
        if (password.length < 6) {
            return SignupResult.Failure("Password must be at least 6 characters")
        }
        if (displayName.isBlank()) {
            return SignupResult.Failure("Please enter your name")
        }

        val existingUser = userDao.getUserByEmail(email.trim().lowercase())
        if (existingUser != null) {
            return SignupResult.Failure("An account with this email already exists")
        }

        val passwordHash = hashPassword(password)
        val user = UserEntity(
            userId = UUID.randomUUID().toString(),
            email = email.trim().lowercase(),
            displayName = displayName.trim(),
            passwordHash = passwordHash,
            profilePicture = profilePicture,
            createdAt = System.currentTimeMillis(),
            lastLogin = System.currentTimeMillis(),
            isPremium = true,
            userType = "premium",
            preferences = "{}"
        )

        return try {
            userDao.insertUser(user)
            createWelcomeMessages(user.userId)
            logActivity(user.userId, "signup", mapOf("email" to user.email))
            loginUser(user)
            SignupResult.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Signup error", e)
            SignupResult.Failure("Signup failed: ${e.message}")
        }
    }

    suspend fun login(email: String, password: String): LoginResult {
        val user = userDao.getUserByEmail(email.trim().lowercase()) ?: return LoginResult.Failure("No account found for this email")

        if (!verifyPassword(password, user.passwordHash)) {
            return LoginResult.Failure("Incorrect password")
        }

        user.lastLogin = System.currentTimeMillis()
        userDao.updateUser(user)
        loginUser(user)
        logActivity(user.userId, "login", mapOf("method" to "email"))

        return LoginResult.Success(user)
    }

    private suspend fun loginUser(user: UserEntity) {
        val session = SessionEntity(
            userId = user.userId,
            token = UUID.randomUUID().toString()
        )
        sessionDao.insertSession(session)

        prefs.edit()
            .putString(PREF_CURRENT_USER, user.userId)
            .putString(PREF_SESSION_TOKEN, session.token)
            .putBoolean(PREF_GUEST_MODE, false)
            .apply()

        _currentUser.value = user
        _isGuestMode.value = false
    }

    suspend fun guestMode(): UserEntity {
        val guestId = "guest_${System.currentTimeMillis()}"
        val guestUser = UserEntity(
            userId = guestId,
            email = "guest@maya.local",
            displayName = "Guest User",
            passwordHash = "",
            createdAt = System.currentTimeMillis(),
            lastLogin = System.currentTimeMillis(),
            isPremium = false,
            userType = "guest",
            preferences = "{}"
        )

        userDao.insertUser(guestUser)
        prefs.edit()
            .putString(PREF_CURRENT_USER, guestId)
            .putBoolean(PREF_GUEST_MODE, true)
            .apply()

        _currentUser.value = guestUser
        _isGuestMode.value = true

        createWelcomeMessages(guestId)
        logActivity(guestId, "guest_login")
        return guestUser
    }

    suspend fun logout() {
        val user = _currentUser.value
        if (user != null) {
            sessionDao.invalidateAllSessions(user.userId)
            logActivity(user.userId, "logout")
        }

        prefs.edit()
            .remove(PREF_CURRENT_USER)
            .remove(PREF_SESSION_TOKEN)
            .putBoolean(PREF_GUEST_MODE, false)
            .apply()

        _currentUser.value = null
        _isGuestMode.value = false
    }

    suspend fun deleteAccount(): Boolean {
        val user = _currentUser.value ?: return false
        return try {
            userDao.deleteUser(user.userId)
            apiKeyDao.deleteAllApiKeys(user.userId)
            welcomeMessageDao.deleteAllWelcomeMessages(user.userId)
            sessionDao.invalidateAllSessions(user.userId)
            logout()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Account deletion error", e)
            false
        }
    }

    // API Key Storage (Encrypted AES/GCM)
    suspend fun saveApiKey(serviceName: String, apiKey: String): Boolean {
        val user = _currentUser.value ?: return false
        return try {
            val encrypted = encryptionManager.encryptApiKey(apiKey.trim())
            val existing = apiKeyDao.getApiKey(user.userId, serviceName.trim().lowercase())

            if (existing != null) {
                existing.encryptedKey = encrypted
                existing.lastUsed = System.currentTimeMillis()
                existing.isActive = true
                apiKeyDao.updateApiKey(existing)
            } else {
                val newKey = ApiKeyEntity(
                    userId = user.userId,
                    serviceName = serviceName.trim().lowercase(),
                    encryptedKey = encrypted
                )
                apiKeyDao.insertApiKey(newKey)
            }
            logActivity(user.userId, "save_api_key", mapOf("service" to serviceName))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save API key", e)
            false
        }
    }

    suspend fun getApiKey(serviceName: String): String? {
        val user = _currentUser.value ?: return null
        val key = apiKeyDao.getApiKey(user.userId, serviceName.trim().lowercase())
        return key?.let {
            encryptionManager.decryptApiKey(it.encryptedKey)
        }
    }

    suspend fun deleteApiKey(serviceName: String): Boolean {
        val user = _currentUser.value ?: return false
        return try {
            val key = apiKeyDao.getApiKey(user.userId, serviceName.trim().lowercase())
            if (key != null) {
                apiKeyDao.deleteApiKey(key.id)
                logActivity(user.userId, "delete_api_key", mapOf("service" to serviceName))
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAllApiKeys(): List<ApiKeyEntity> {
        val user = _currentUser.value ?: return emptyList()
        return apiKeyDao.getApiKeys(user.userId)
    }

    // Welcome Messages & Creator Credit
    private suspend fun createWelcomeMessages(userId: String) {
        val messages = listOf(
            "👋 Welcome to MAYA! I'm your autonomous AI assistant.",
            "🌅 Good to see you! MAYA is synchronized and ready.",
            "🚀 MAYA initialized. Ready for hands-free operations.",
            "💡 Tip: Say 'Hey Maya' anytime to command your phone.",
            "🎯 I can control your apps, smart home, health, and more!",
            "🌟 Made by Mani with ❤️"
        )
        messages.forEach { msg ->
            val entity = WelcomeMessageEntity(
                userId = userId,
                message = msg,
                type = if (msg.contains("Mani")) "creator" else "welcome"
            )
            welcomeMessageDao.insertWelcomeMessage(entity)
        }
    }

    suspend fun getWelcomeMessage(): String {
        val user = _currentUser.value
        val name = user?.displayName ?: "Explorer"
        return "👋 Welcome, $name!\n\nMAYA is standing by for your commands. Control apps, play media, manage device settings, and automate your workflow effortlessly.\n\n🌟 Made by Mani with ❤️"
    }

    fun getCreatorCredit(): String = "🌟 MADE BY MANI"

    fun getDailyMessage(): String {
        val messages = listOf(
            "🌅 Good morning! Ready to conquer today with MAYA?",
            "🌤️ Afternoon boost: Your personal AI assistant is active.",
            "🌙 Evening check: Wind down and let MAYA automate your routines.",
            "🌟 Every day is a canvas of possibilities. Let's make it count!",
            "💪 Peak performance: All systems synchronized for you.",
            "🚀 Let's turn your ideas into reality today!",
            "⚡ High energy, zero downtime. What's our next goal?"
        )
        val index = ((System.currentTimeMillis() / (1000 * 60 * 60 * 24)) % messages.size).toInt()
        return "${messages[index]}\n\n🌟 Made by Mani with ❤️"
    }

    fun getMotivationalMessage(): String {
        val quotes = listOf(
            "✨ You are capable of extraordinary things.",
            "🚀 The future belongs to those who build it.",
            "💪 Discipline turns intentions into unstoppable momentum.",
            "🎯 Focus on progress, not perfection.",
            "🌟 Greatness is unlocked one decision at a time."
        )
        val idx = ((System.currentTimeMillis() / (1000 * 60 * 60)) % quotes.size).toInt()
        return quotes[idx]
    }

    // Activity Logging
    suspend fun logActivity(userId: String, action: String, details: Map<String, Any> = emptyMap()) {
        try {
            val json = JSONObject(details).toString()
            val activity = UserActivityEntity(
                userId = userId,
                action = action,
                details = json
            )
            activityDao.insertActivity(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging activity", e)
        }
    }

    suspend fun getRecentActivities(limit: Int = 10): List<UserActivityEntity> {
        val user = _currentUser.value ?: return emptyList()
        return activityDao.getRecentActivities(user.userId, limit)
    }

    // Password Hashing
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }
}
