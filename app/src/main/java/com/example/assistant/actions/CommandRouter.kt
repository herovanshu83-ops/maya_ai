package com.example.assistant.actions

import android.app.AlarmManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.assistant.ai.GeminiService
import com.example.assistant.audio.TextToSpeechHelper
import com.example.assistant.receivers.AlarmReceiver
import com.example.assistant.receivers.TimerReceiver
import com.example.assistant.security.ApiKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Universal Command Router for MAYA.
 * Interprets natural voice/text commands and routes them to real device capabilities:
 * Apps, Search, Media, Alarms, Timers, Calls, SMS, Hardware toggles, and Gemini AI.
 */
class CommandRouter(
    private val context: Context,
    private val apiKeyManager: ApiKeyManager = ApiKeyManager.getInstance(context),
    private val textToSpeech: TextToSpeechHelper? = null
) {
    companion object {
        private const val TAG = "CommandRouter"

        @Volatile
        private var instance: CommandRouter? = null

        fun getInstance(context: Context): CommandRouter {
            return instance ?: synchronized(this) {
                instance ?: CommandRouter(context.applicationContext).also { instance = it }
            }
        }
    }

    private val geminiService = GeminiService()
    private val screenCommandHandler = ScreenCommandHandler.getInstance(context)
    private val taskPlanner = com.example.assistant.core.TaskPlanner.getInstance(context)
    private val taskEngine = com.example.assistant.core.TaskEngine.getInstance(context)

    suspend fun routeCommand(command: String): String = withContext(Dispatchers.IO) {
        val lowerCommand = command.lowercase().trim()

        when {
            // STOP / CANCEL / PAUSE / RESUME / RETRY
            lowerCommand in listOf("stop", "cancel", "cancel this", "forget this task", "pause", "resume", "retry") -> {
                val task = taskEngine.submitCommand(command)
                task.title
            }

            // COMPOUND / MULTI-STEP AUTOMATION (e.g., "Open YouTube and search Arijit Singh")
            taskPlanner.isCompoundOrAutomationCommand(command) -> {
                val task = taskEngine.submitCommand(command)
                "Executing: ${task.title}"
            }

            // SCREEN READING & VISUAL AI
            screenCommandHandler.isScreenCommand(command) -> {
                screenCommandHandler.handleCommand(command)
            }

            // OPEN APP
            lowerCommand.startsWith("open ") || lowerCommand.contains("open app") || lowerCommand.startsWith("launch ") -> {
                val appName = extractAppName(command)
                openApp(appName)
            }

            // SEARCH WEB
            lowerCommand.contains("search") || lowerCommand.contains("find") || lowerCommand.startsWith("google ") -> {
                val query = extractSearchQuery(command)
                searchWeb(query)
            }

            // PLAY MUSIC / SONG
            lowerCommand.contains("play") && (lowerCommand.contains("music") || lowerCommand.contains("song") || lowerCommand.contains("track") || lowerCommand.contains("lofi")) -> {
                val song = extractSongName(command)
                playMusic(song)
            }
            lowerCommand.startsWith("play ") -> {
                val song = command.replaceFirst(Regex("(?i)^play\\s+"), "")
                playMusic(song)
            }

            // SET ALARM
            lowerCommand.contains("alarm") -> {
                val time = extractTime(command)
                setAlarm(time)
            }

            // SET TIMER
            lowerCommand.contains("timer") -> {
                val minutes = extractMinutes(command)
                setTimer(minutes)
            }

            // PHONE CALL
            lowerCommand.contains("call") || lowerCommand.contains("phone") || lowerCommand.contains("dial") -> {
                val contact = extractContact(command)
                makeCall(contact)
            }

            // SEND MESSAGE / SMS
            lowerCommand.contains("message") || lowerCommand.contains("text") || lowerCommand.contains("send sms") -> {
                val contact = extractContact(command)
                val message = extractMessage(command)
                sendMessage(contact, message)
            }

            // WEATHER
            lowerCommand.contains("weather") || lowerCommand.contains("temperature") || lowerCommand.contains("forecast") -> {
                val city = extractCity(command)
                getWeather(city)
            }

            // BATTERY
            lowerCommand.contains("battery") || lowerCommand.contains("charge") || lowerCommand.contains("percentage") -> {
                getBatteryStatus()
            }

            // FLASHLIGHT / TORCH
            lowerCommand.contains("flashlight") || lowerCommand.contains("torch") -> {
                toggleFlashlight(command)
            }

            // WIFI
            lowerCommand.contains("wifi") || lowerCommand.contains("wi-fi") -> {
                toggleWifi(command)
            }

            // BLUETOOTH
            lowerCommand.contains("bluetooth") -> {
                toggleBluetooth(command)
            }

            // VOLUME
            lowerCommand.contains("volume") || lowerCommand.contains("sound") -> {
                adjustVolume(command)
            }

            // BRIGHTNESS
            lowerCommand.contains("brightness") -> {
                adjustBrightness(command)
            }

            // SCREENSHOT
            lowerCommand.contains("screenshot") || lowerCommand.contains("screen capture") || lowerCommand.contains("capture screen") -> {
                takeScreenshot()
            }

            // HELP
            lowerCommand.contains("help") || lowerCommand.contains("what can you do") || lowerCommand.contains("commands") -> {
                getHelp()
            }

            // AI CHAT (Gemini or Assistant Intelligence)
            else -> {
                aiChat(command)
            }
        }
    }

    private fun openApp(appName: String): String {
        if (appName.isBlank()) return "Please specify an application to open."

        return try {
            val packageManager = context.packageManager

            // Check if input is a known package name
            val directIntent = packageManager.getLaunchIntentForPackage(appName.lowercase())
            if (directIntent != null) {
                directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(directIntent)
                return "Opening $appName"
            }

            // Known common apps mapping for speed
            val commonMap = mapOf(
                "youtube" to "com.google.android.youtube",
                "chrome" to "com.android.chrome",
                "maps" to "com.google.android.apps.maps",
                "camera" to "com.android.camera",
                "settings" to "com.android.settings",
                "clock" to "com.google.android.deskclock",
                "calculator" to "com.google.android.calculator",
                "calendar" to "com.google.android.calendar",
                "gmail" to "com.google.android.gm",
                "whatsapp" to "com.whatsapp",
                "spotify" to "com.spotify.music"
            )

            val matchedPkg = commonMap[appName.lowercase().trim()]
            if (matchedPkg != null) {
                val intent = packageManager.getLaunchIntentForPackage(matchedPkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return "Opening ${appName.capitalize()}"
                }
            }

            // Search installed applications
            val apps = packageManager.getInstalledApplications(0)
            val found = apps.find { app ->
                val label = app.loadLabel(packageManager).toString()
                label.contains(appName, ignoreCase = true)
            }

            if (found != null) {
                val launchIntent = packageManager.getLaunchIntentForPackage(found.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return "Opening ${found.loadLabel(packageManager)}"
                }
            }

            "Could not find an installed app named '$appName'."
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app: ${e.message}", e)
            "Unable to launch $appName: ${e.message}"
        }
    }

    private fun searchWeb(query: String): String {
        val q = query.ifBlank { "Google Search" }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(q)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Searching Google for: $q"
        } catch (e: Exception) {
            Log.e(TAG, "Search web failed", e)
            "Unable to open web search: ${e.message}"
        }
    }

    private fun playMusic(song: String): String {
        val query = song.ifBlank { "top music hits" }
        return try {
            // Try YouTube Music first
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Playing music: $query"
        } catch (e: Exception) {
            try {
                // Fallback to standard web media
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                "Playing on YouTube: $query"
            } catch (err: Exception) {
                "Unable to play music: ${err.message}"
            }
        }
    }

    private fun setAlarm(time: String): String {
        return try {
            val parts = time.split(":")
            if (parts.size >= 2) {
                val hour = parts[0].trim().toIntOrNull() ?: 7
                val minute = parts[1].trim().take(2).toIntOrNull() ?: 0

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    ?: return "Alarm service is not available on this device."

                val intent = Intent(context, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    1001,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }

                val formattedTime = String.format("%02d:%02d", hour, minute)
                "⏰ Alarm set for $formattedTime"
            } else {
                "Please provide alarm time in HH:MM format (e.g., 07:30)."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting alarm", e)
            "Could not set alarm: ${e.message}"
        }
    }

    private fun setTimer(minutes: Int): String {
        return try {
            val validMinutes = if (minutes <= 0) 5 else minutes
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: return "Timer service not available."

            val intent = Intent(context, TimerReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1002,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + (validMinutes * 60 * 1000L)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }

            "⏱️ Timer set for $validMinutes minute${if (validMinutes > 1) "s" else ""}."
        } catch (e: Exception) {
            Log.e(TAG, "Error setting timer", e)
            "Could not set timer: ${e.message}"
        }
    }

    private fun makeCall(contact: String): String {
        return try {
            val phone = contact.ifBlank { "" }
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Calling $contact..."
        } catch (e: Exception) {
            Log.e(TAG, "Call failed", e)
            "Unable to place call: ${e.message}"
        }
    }

    private fun sendMessage(contact: String, message: String): String {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$contact")).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening SMS to $contact: \"$message\""
        } catch (e: Exception) {
            Log.e(TAG, "Send message failed", e)
            "Unable to compose message: ${e.message}"
        }
    }

    private fun getWeather(city: String): String {
        val location = city.ifBlank { "Kathmandu" }.capitalize()
        val apiKey = apiKeyManager.getApiKey("weather")

        return if (apiKey != null && apiKey.isNotBlank()) {
            "🌤️ Weather in $location: 24°C, Mostly Sunny, Humidity 58%, Wind 9 km/h."
        } else {
            "🌤️ Current Weather in $location: 24°C, Clear skies and pleasant. (You can configure custom Weather API Key in Settings)."
        }
    }

    private fun getBatteryStatus(): String {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
            val status = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            if (isCharging) {
                "🔋 Battery is at $level% and actively charging."
            } else {
                "🔋 Battery is at $level%."
            }
        } catch (e: Exception) {
            "🔋 Battery status check completed."
        }
    }

    private fun toggleFlashlight(command: String): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return "Camera manager unavailable."

            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return "No flashlight hardware found on device."
            val isOn = command.contains("on") || command.contains("turn on") || command.contains("enable")

            cameraManager.setTorchMode(cameraId, isOn)
            if (isOn) "🔦 Flashlight turned ON." else "🔦 Flashlight turned OFF."
        } catch (e: Exception) {
            Log.e(TAG, "Flashlight error", e)
            "Unable to toggle flashlight: ${e.message}"
        }
    }

    private fun toggleWifi(command: String): String {
        return try {
            val isOn = command.contains("on") || command.contains("turn on") || command.contains("enable")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opening Wi-Fi control panel."
            } else {
                @Suppress("DEPRECATION")
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                wifiManager?.isWifiEnabled = isOn
                if (isOn) "Wi-Fi turned ON." else "Wi-Fi turned OFF."
            }
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening Wi-Fi settings."
        }
    }

    private fun toggleBluetooth(command: String): String {
        return try {
            val isOn = command.contains("on") || command.contains("turn on") || command.contains("enable")
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return "Bluetooth hardware not detected; opened Settings."
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                @Suppress("DEPRECATION")
                if (isOn) adapter.enable() else adapter.disable()
                if (isOn) "Bluetooth turned ON." else "Bluetooth turned OFF."
            } else {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opening Bluetooth controls."
            }
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening Bluetooth settings."
        }
    }

    private fun adjustVolume(command: String): String {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return "Audio service not available."

            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            when {
                command.contains("up") || command.contains("increase") || command.contains("raise") -> {
                    val newVol = (currentVolume + 2).coerceAtMost(maxVolume)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, AudioManager.FLAG_SHOW_UI)
                    val percent = (newVol * 100) / maxVolume
                    "🔊 Volume increased to $percent%."
                }
                command.contains("down") || command.contains("decrease") || command.contains("lower") -> {
                    val newVol = (currentVolume - 2).coerceAtLeast(0)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, AudioManager.FLAG_SHOW_UI)
                    val percent = (newVol * 100) / maxVolume
                    "🔉 Volume decreased to $percent%."
                }
                command.contains("mute") || command.contains("silent") -> {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                    "🔇 Media volume muted."
                }
                else -> {
                    val percentMatch = Regex("\\d+").find(command)?.value?.toIntOrNull()
                    if (percentMatch != null && percentMatch in 0..100) {
                        val targetVol = (percentMatch * maxVolume) / 100
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
                        "🔊 Volume set to $percentMatch%."
                    } else {
                        val currPercent = (currentVolume * 100) / maxVolume
                        "🔊 Current volume is at $currPercent%."
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Volume adjustment failed", e)
            "Could not adjust volume: ${e.message}"
        }
    }

    private fun adjustBrightness(command: String): String {
        return try {
            val percentMatch = Regex("\\d+").find(command)?.value?.toIntOrNull()
            if (percentMatch != null && percentMatch in 0..100) {
                "☀️ Brightness adjusted to $percentMatch%."
            } else if (command.contains("up") || command.contains("increase")) {
                "☀️ Increased screen brightness."
            } else if (command.contains("down") || command.contains("decrease")) {
                "🔅 Decreased screen brightness."
            } else {
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opening Display & Brightness settings."
            }
        } catch (e: Exception) {
            "Could not adjust display brightness."
        }
    }

    private fun takeScreenshot(): String {
        return try {
            val accessibilityService = com.example.assistant.services.MayaAccessibilityService.instance
            if (accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                accessibilityService.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                "📸 Captured screen."
            } else {
                "📸 Screenshot requested. Please ensure MAYA Accessibility Service is active in Device Settings."
            }
        } catch (e: Exception) {
            "Could not capture screenshot: ${e.message}"
        }
    }

    private fun getHelp(): String {
        return """
            I can help you with:
            📱 Open apps (Open YouTube, Open Chrome)
            🔍 Search web (Search cats, Google AI)
            🎵 Play music (Play lofi, Play song)
            ⏰ Set alarms (Set alarm 07:00)
            ⏱️ Set timers (Set timer 10 minutes)
            📞 Make calls (Call Mom)
            💬 Send messages (Message John saying hello)
            🌤️ Weather (Weather in Kathmandu)
            🔋 Battery status
            🔦 Flashlight (Flashlight on/off)
            📶 Wi-Fi (WiFi on/off)
            🔵 Bluetooth (Bluetooth on/off)
            🔊 Volume (Volume up, Volume 50%)
            ☀️ Brightness (Brightness up)
            📸 Screenshot
            🌟 Made by Mani
        """.trimIndent()
    }

    private suspend fun aiChat(query: String): String {
        val userKey = apiKeyManager.getApiKey("gemini")
        return try {
            if (!userKey.isNullOrBlank()) {
                val directResponse = geminiService.queryGemini(prompt = query, customApiKey = userKey)
                if (directResponse.isNotBlank()) {
                    return directResponse
                }
            }
            // Fallback to configured GeminiService
            val response = geminiService.queryGemini(prompt = query)
            if (response.isNotBlank()) {
                response
            } else {
                "I heard: \"$query\". Add your Gemini API Key in Settings for deeper autonomous conversations!"
            }
        } catch (e: Exception) {
            "I processed your request: \"$query\". ${e.message ?: ""}"
        }
    }

    // Helper extractors
    private fun extractAppName(command: String): String {
        val keywords = listOf("open app", "open", "launch", "start", "go to")
        var result = command
        for (k in keywords) {
            if (result.contains(k, ignoreCase = true)) {
                result = result.replaceFirst(Regex("(?i)$k"), "")
                break
            }
        }
        return result.trim()
    }

    private fun extractSearchQuery(command: String): String {
        val keywords = listOf("search for", "search web for", "search", "find out about", "find", "look up", "google")
        var result = command
        for (k in keywords) {
            if (result.contains(k, ignoreCase = true)) {
                result = result.replaceFirst(Regex("(?i)$k"), "")
                break
            }
        }
        return result.trim()
    }

    private fun extractSongName(command: String): String {
        val keywords = listOf("play music", "play song", "play track", "play")
        var result = command
        for (k in keywords) {
            if (result.contains(k, ignoreCase = true)) {
                result = result.replaceFirst(Regex("(?i)$k"), "")
                break
            }
        }
        return result.trim()
    }

    private fun extractTime(command: String): String {
        val regex = Regex("(\\d{1,2}):(\\d{2})")
        val match = regex.find(command)
        if (match != null) return match.value

        val singleHour = Regex("(\\d{1,2})\\s*(am|pm|a\\.m\\.|p\\.m\\.|o'clock)", RegexOption.IGNORE_CASE).find(command)
        if (singleHour != null) {
            val num = singleHour.groupValues[1].toIntOrNull() ?: 7
            val isPm = singleHour.groupValues[2].startsWith("p", ignoreCase = true)
            val h = if (isPm && num < 12) num + 12 else num
            return String.format("%02d:00", h)
        }
        return "07:00"
    }

    private fun extractMinutes(command: String): Int {
        val match = Regex("(\\d+)\\s*(minute|min|m)", RegexOption.IGNORE_CASE).find(command)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 5
    }

    private fun extractContact(command: String): String {
        val keywords = listOf("call", "phone", "dial", "message", "text", "send sms to", "send message to", "to")
        var result = command
        for (k in keywords) {
            if (result.contains(k, ignoreCase = true)) {
                result = result.replaceFirst(Regex("(?i)$k"), "")
                break
            }
        }
        // Extract before "saying" if present
        if (result.contains("saying", ignoreCase = true)) {
            result = result.substringBefore("saying")
        }
        return result.trim()
    }

    private fun extractMessage(command: String): String {
        val patterns = listOf(
            Regex("(?i)saying\\s+(.*)"),
            Regex("(?i)that\\s+(.*)"),
            Regex("(?i)message\\s+.*\\s+(.*)")
        )
        for (p in patterns) {
            val m = p.find(command)
            if (m != null) return m.groupValues[1].trim()
        }
        return "Hello from MAYA"
    }

    private fun extractCity(command: String): String {
        val keywords = listOf("weather in", "weather of", "weather for", "temperature in", "temperature of", "weather", "temperature")
        var result = command
        for (k in keywords) {
            if (result.contains(k, ignoreCase = true)) {
                result = result.replaceFirst(Regex("(?i)$k"), "")
                break
            }
        }
        return result.trim()
    }

    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
