package com.example.assistant.user

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WelcomeManager private constructor(
    private val context: Context,
    private val userManager: UserManager
) {

    companion object {
        private const val PREFS_NAME = "maya_welcome_prefs"
        private const val PREF_FIRST_LAUNCH = "first_launch"
        private const val PREF_LAST_WELCOME_DATE = "last_welcome_date"

        @Volatile
        private var instance: WelcomeManager? = null

        fun getInstance(context: Context): WelcomeManager {
            return instance ?: synchronized(this) {
                val userMgr = UserManager.getInstance(context)
                instance ?: WelcomeManager(context.applicationContext, userMgr).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _welcomeMessage = MutableStateFlow("")
    val welcomeMessage: StateFlow<String> = _welcomeMessage.asStateFlow()

    private val _dailyMessage = MutableStateFlow(userManager.getDailyMessage())
    val dailyMessage: StateFlow<String> = _dailyMessage.asStateFlow()

    private val _motivationalMessage = MutableStateFlow(userManager.getMotivationalMessage())
    val motivationalMessage: StateFlow<String> = _motivationalMessage.asStateFlow()

    private val _creatorCredit = MutableStateFlow(userManager.getCreatorCredit())
    val creatorCredit: StateFlow<String> = _creatorCredit.asStateFlow()

    private val _showWelcome = MutableStateFlow(false)
    val showWelcome: StateFlow<Boolean> = _showWelcome.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            checkAndTriggerWelcome()
        }
    }

    private fun checkAndTriggerWelcome() {
        val isFirstLaunch = prefs.getBoolean(PREF_FIRST_LAUNCH, true)
        val lastDate = prefs.getString(PREF_LAST_WELCOME_DATE, "")
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (isFirstLaunch) {
            _welcomeMessage.value = getFirstLaunchWelcome()
            _showWelcome.value = true
            prefs.edit()
                .putBoolean(PREF_FIRST_LAUNCH, false)
                .putString(PREF_LAST_WELCOME_DATE, today)
                .apply()
        } else if (lastDate != today) {
            _welcomeMessage.value = getDailyWelcome()
            _showWelcome.value = true
            prefs.edit()
                .putString(PREF_LAST_WELCOME_DATE, today)
                .apply()
        }

        _dailyMessage.value = userManager.getDailyMessage()
        _motivationalMessage.value = userManager.getMotivationalMessage()
        _creatorCredit.value = userManager.getCreatorCredit()
    }

    fun dismissWelcome() {
        _showWelcome.value = false
    }

    fun triggerWelcomeManually() {
        _welcomeMessage.value = getStandardWelcome()
        _showWelcome.value = true
    }

    private fun getFirstLaunchWelcome(): String {
        return """
            🎉 Welcome to MAYA!
            
            Your autonomous AI Voice Assistant is ready to control your device hands-free.
            
            Key Capabilities:
            📱 Control device settings & toggle tools
            💬 Instant messaging & direct phone calls
            🎵 Media playback & volume controls
            🧠 Real-time Gemini AI voice responses
            🏡 Smart Home, Tesla & Gaming booster
            
            🌟 MADE BY MANI
            
            Say "Hey Maya" to begin!
        """.trimIndent()
    }

    private fun getStandardWelcome(): String {
        val name = userManager.currentUser.value?.displayName ?: "User"
        return """
            👋 Welcome back, $name!
            
            MAYA is standing by and fully synchronized.
            
            Say "Hey Maya" followed by your command anytime.
            
            🌟 MADE BY MANI
        """.trimIndent()
    }

    private fun getDailyWelcome(): String {
        val day = getDayOfWeek()
        val time = getTimeOfDay()
        val name = userManager.currentUser.value?.displayName ?: "there"

        return """
            🌅 Good $time, $name!
            
            It's $day. Here is your daily spark:
            "${userManager.getMotivationalMessage()}"
            
            🌟 MADE BY MANI
        """.trimIndent()
    }

    private fun getDayOfWeek(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Today"
        }
    }

    private fun getTimeOfDay(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..21 -> "evening"
            else -> "night"
        }
    }
}
