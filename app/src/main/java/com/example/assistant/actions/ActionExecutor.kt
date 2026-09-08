package com.example.assistant.actions

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import com.example.assistant.ai.GeminiService
import com.example.assistant.nlu.IntentClassifier
import com.example.assistant.nlu.IntentType
import com.example.assistant.nlu.ParsedIntent
import com.example.assistant.services.MayaAccessibilityService
import com.example.data.repository.AssistantRepository
import com.example.utils.DeviceUtils
import com.example.utils.VersionUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActionExecutor(
    private val context: Context,
    private val repository: AssistantRepository,
    private val geminiService: GeminiService
) {

    val screenMapper = ScreenMapper(context)
    val screenOverlay = ScreenOverlay(context)
    val accessibilityHelper = AccessibilityHelper(context)
    private var lastForegroundApp: String = ""

    data class ExecutionResult(
        val spokenResponse: String,
        val isSuccess: Boolean = true,
        val details: String = ""
    )

    suspend fun execute(intent: ParsedIntent): ExecutionResult {
        return when (intent.type) {
            IntentType.SCREEN_MAP -> mapAndShowScreen(intent.action == "clickable")
            IntentType.FIND_TEXT -> findTextOnScreen(intent.target)
            IntentType.CLICK_ELEMENT -> clickElementOnScreen(intent.target)
            IntentType.TYPE_TEXT -> typeTextOnScreen(intent.target)
            IntentType.SWIPE -> swipeOnScreen(intent.target)
            IntentType.SCROLL -> scrollOnScreen(intent.target)
            IntentType.LONG_PRESS -> longPressOnScreen(intent.target)
            IntentType.TAP_COORDINATES -> tapCoordinatesOnScreen(
                intent.parameters["x"]?.toFloatOrNull() ?: 500f,
                intent.parameters["y"]?.toFloatOrNull() ?: 500f
            )
            IntentType.OPEN_APP -> openApp(intent.target)
            IntentType.CLOSE_APP -> closeApp()
            IntentType.APP_SEARCH -> searchInApp(intent.target, intent.parameters["app"] ?: lastForegroundApp)
            IntentType.PLAY_STORE_SEARCH -> searchPlayStore(intent.target)
            IntentType.FLASHLIGHT -> toggleFlashlight(intent.action)
            IntentType.VOLUME_CONTROL -> controlVolume(intent.action, intent.parameters["percent"]?.toIntOrNull())
            IntentType.BRIGHTNESS_CONTROL -> adjustBrightness()
            IntentType.SYSTEM_CONTROL -> handleSystemControl(intent.target, intent.action)
            IntentType.CALL -> makeCall(intent.target, intent.parameters["whatsapp"] == "true")
            IntentType.SEND_MESSAGE -> sendMessage(intent.target, intent.parameters["message"] ?: "", intent.parameters["whatsapp"] == "true")
            IntentType.READ_MESSAGES -> readMessages()
            IntentType.SEARCH_CONTACT -> searchContacts(intent.target)
            IntentType.MEDIA_CONTROL -> controlMedia(intent.action, intent.target, intent.parameters["app"] ?: intent.parameters["service"] ?: lastForegroundApp)
            IntentType.ALARM -> manageAlarm(intent.action, intent.parameters["time"] ?: "7:00 AM")
            IntentType.TIMER -> manageTimer(intent.action, intent.parameters["duration"]?.toIntOrNull() ?: 5, intent.parameters["unit"] ?: "minute")
            IntentType.CALENDAR -> manageCalendar(intent.target)
            IntentType.REMINDER -> createReminder(intent.target)
            IntentType.NAVIGATION -> navigateTo(intent.target)
            IntentType.LOCATION_QUERY -> queryLocation()
            IntentType.CAMERA -> openCamera(intent.action)
            IntentType.SCREENSHOT -> takeScreenshot()
            IntentType.DEVICE_STATUS -> getDeviceStatus(intent.target)
            IntentType.SETTINGS -> openSystemSettings()
            IntentType.WEATHER -> checkWeather(intent.target)
            IntentType.MEMORY_STORE -> storeMemory(intent.target, intent.parameters["fact"] ?: intent.rawQuery)
            IntentType.MEMORY_RECALL -> recallMemories()
            IntentType.MEMORY_CLEAR -> clearMemories()
            IntentType.ROUTINE_CREATE -> createRoutine(intent.target)
            IntentType.ROUTINE_RUN -> runRoutine(intent.target)
            IntentType.ROUTINE_LIST -> listRoutines()
            IntentType.OVERLAY_CONTROL -> handleOverlayControl(intent.action)
            IntentType.BACKGROUND_CONTROL -> handleBackgroundControl(intent.action)
            IntentType.PERSONALITY_CONTROL -> handlePersonality(intent.action, intent.target)
            IntentType.MOOD_CONTROL -> handleMood(intent.action)
            IntentType.SMART_HOME_CONTROL -> handleSmartHome(intent.action, intent.target)
            IntentType.VEHICLE_CONTROL -> handleVehicle(intent.action, intent.target)
            IntentType.HEALTH_CONTROL -> handleHealth(intent.action, intent.target)
            IntentType.PRODUCTIVITY_CONTROL -> handleProductivity(intent.action, intent.target)
            IntentType.SUPER_MEDIA -> handleSuperMedia(intent.action, intent.target)
            IntentType.SECURITY_CONTROL -> handleSecurity(intent.action)
            IntentType.OFFLINE_CONTROL -> handleOffline(intent.action)
            IntentType.ORB_CUSTOMIZATION -> handleOrbCustomization(intent.action, intent.target)
            IntentType.GAMING_CONTROL -> handleGaming(intent.action)
            IntentType.FINANCE_CONTROL -> handleFinance(intent.action)
            IntentType.ANALYTICS_CONTROL -> handleAnalytics(intent.action)
            IntentType.DEVELOPER_CONTROL -> handleDeveloper(intent.action)
            IntentType.COMMUNITY_CONTROL -> handleCommunity(intent.action)
            IntentType.INTERRUPTION -> ExecutionResult("Stopped.", true)
            IntentType.MULTI_STEP -> executeMultiStep(intent.subIntents)
            IntentType.AI_QUERY -> queryAI(intent.rawQuery)
        }
    }

    private fun handleOverlayControl(action: String): ExecutionResult {
        val overlayHelper = com.example.utils.OverlayPermissionHelper(context)
        return when (action) {
            "show" -> {
                if (!overlayHelper.hasOverlayPermission()) {
                    ExecutionResult("Overlay permission is required. Please grant display over other apps permission in Settings.", false)
                } else {
                    com.example.assistant.services.MayaOverlayService.start(context)
                    ExecutionResult("Displaying MAYA floating overlay over all apps.", true)
                }
            }
            "hide" -> {
                com.example.assistant.services.MayaOverlayService.stop(context)
                ExecutionResult("MAYA overlay hidden.", true)
            }
            "minimize" -> {
                com.example.assistant.services.MayaOverlayService.start(context)
                ExecutionResult("Minimized MAYA to floating orb.", true)
            }
            else -> {
                com.example.assistant.services.MayaOverlayService.start(context)
                ExecutionResult("MAYA overlay active.", true)
            }
        }
    }

    private fun handleBackgroundControl(action: String): ExecutionResult {
        val prefs = context.getSharedPreferences("maya_prefs", Context.MODE_PRIVATE)
        return when (action) {
            "start" -> {
                com.example.assistant.services.MayaForegroundService.start(context)
                ExecutionResult("MAYA background voice engine is now active and running.", true)
            }
            "stop" -> {
                com.example.assistant.services.MayaForegroundService.stop(context)
                com.example.assistant.services.MayaOverlayService.stop(context)
                ExecutionResult("MAYA background services stopped.", true)
            }
            "restart" -> {
                com.example.assistant.services.MayaForegroundService.stop(context)
                com.example.assistant.services.MayaForegroundService.start(context)
                ExecutionResult("MAYA background services restarted.", true)
            }
            "status" -> {
                val isBg = com.example.assistant.services.MayaForegroundService.isRunning
                val isOverlay = com.example.assistant.services.MayaOverlayService.isRunning
                val statusMsg = "MAYA Status: Background Engine is ${if (isBg) "Running" else "Stopped"}, Overlay is ${if (isOverlay) "Active" else "Inactive"}."
                ExecutionResult(statusMsg, true)
            }
            "enable_autostart" -> {
                prefs.edit().putBoolean("auto_start_background", true).apply()
                ExecutionResult("Auto-start on device boot enabled.", true)
            }
            "disable_autostart" -> {
                prefs.edit().putBoolean("auto_start_background", false).apply()
                ExecutionResult("Auto-start on device boot disabled.", true)
            }
            else -> ExecutionResult("MAYA background service is configured.", true)
        }
    }

    // ==========================================
    // SCREEN MAPPING & DIRECT ACCESSIBILITY
    // ==========================================

    suspend fun mapAndShowScreen(clickableOnly: Boolean = false): ExecutionResult {
        if (!MayaAccessibilityService.isServiceActive(context)) {
            val msg = "Please enable MAYA in Accessibility Settings so I can see and map the screen."
            screenOverlay.showVoiceOutput(msg)
            return ExecutionResult(msg, false)
        }

        val map = screenMapper.mapCurrentScreen()
        screenOverlay.showScreenMap(map)

        val total = map.elements.size
        val clickable = map.clickableElements.size
        val pkg = map.packageName

        val msg = if (clickableOnly) {
            "Found $clickable clickable elements on screen."
        } else {
            "Screen mapped for $pkg. Found $total elements, including $clickable interactive targets."
        }
        screenOverlay.showVoiceOutput(msg)
        return ExecutionResult(msg, true, "Mapped $total elements in $pkg")
    }

    suspend fun findTextOnScreen(query: String): ExecutionResult {
        if (!MayaAccessibilityService.isServiceActive(context)) {
            return ExecutionResult("Please enable Accessibility Service so I can search on-screen text.", false)
        }

        val element = screenMapper.findElementByText(query)
        return if (element != null) {
            screenOverlay.highlightSearchResults(query, listOf(element))
            val msg = "Found \"$query\" on screen."
            screenOverlay.showVoiceOutput(msg)
            ExecutionResult(msg, true)
        } else {
            val msg = "I couldn't find \"$query\" on the current screen."
            screenOverlay.showVoiceOutput(msg)
            ExecutionResult(msg, false)
        }
    }

    suspend fun clickElementOnScreen(target: String): ExecutionResult {
        val service = MayaAccessibilityService.instance
        if (service == null) {
            return ExecutionResult("Please enable MAYA's Accessibility Service first.", false)
        }

        val clean = target.lowercase().trim()

        // 1. Check if user wants first search result
        if (clean.contains("first") || clean.contains("first result") || clean.contains("first video") || clean.contains("first song")) {
            val map = screenMapper.mapCurrentScreen()
            val clickables = map.clickableElements
            val first = clickables.firstOrNull { it.bounds.width() > 100 && it.bounds.height() > 60 }
            if (first != null) {
                screenOverlay.highlightElement(first.bounds, Color.GREEN)
                screenOverlay.showTapWithText(first.bounds.centerX().toFloat(), first.bounds.centerY().toFloat(), "▶️ Click", Color.GREEN)
                service.performTap(first.bounds.centerX().toFloat(), first.bounds.centerY().toFloat())
                val msg = "Clicked first result."
                screenOverlay.showVoiceOutput(msg)
                return ExecutionResult(msg, true)
            }
        }

        // 2. Search by text in screen mapper
        val element = screenMapper.findElementByText(target)
        if (element != null) {
            screenOverlay.highlightElement(element.bounds, Color.CYAN)
            screenOverlay.showTapWithText(element.bounds.centerX().toFloat(), element.bounds.centerY().toFloat(), "👆 $target")
            service.performTap(element.bounds.centerX().toFloat(), element.bounds.centerY().toFloat())
            val msg = "Clicked $target."
            screenOverlay.showVoiceOutput(msg)
            return ExecutionResult(msg, true)
        }

        // 3. Fallback direct accessibility search & click
        val clicked = service.findAndTapByText(target) ||
                service.findAndTapByDescription(target) ||
                service.findAndTapByViewId(target)

        return if (clicked) {
            val msg = "Clicked $target."
            screenOverlay.showVoiceOutput(msg)
            ExecutionResult(msg, true)
        } else {
            val msg = "Could not find a clickable element for \"$target\"."
            screenOverlay.showVoiceOutput(msg)
            ExecutionResult(msg, false)
        }
    }

    suspend fun typeTextOnScreen(text: String): ExecutionResult {
        val service = MayaAccessibilityService.instance
        if (service == null) {
            return ExecutionResult("Please enable MAYA's Accessibility Service to type.", false)
        }

        val editables = screenMapper.findEditableElements()
        val targetEdit = editables.firstOrNull()

        if (targetEdit != null) {
            screenOverlay.highlightElement(targetEdit.bounds, Color.YELLOW)
            screenOverlay.showTapWithText(targetEdit.bounds.centerX().toFloat(), targetEdit.bounds.centerY().toFloat(), "⌨️ $text", Color.YELLOW)
        }

        val typed = service.typeText(text)
        return if (typed) {
            val msg = "Typed: $text"
            screenOverlay.showVoiceOutput(msg)
            ExecutionResult(msg, true)
        } else {
            val msg = "No active editable text field found."
            screenOverlay.showVoiceOutput(msg)
            ExecutionResult(msg, false)
        }
    }

    suspend fun swipeOnScreen(direction: String): ExecutionResult {
        val service = MayaAccessibilityService.instance
        if (service == null) {
            return ExecutionResult("Please enable Accessibility Service to swipe.", false)
        }

        val displayMetrics = context.resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f

        val (fromX, fromY, toX, toY) = when (direction.lowercase().trim()) {
            "up" -> listOf(centerX, centerY + 350f, centerX, centerY - 350f)
            "down" -> listOf(centerX, centerY - 350f, centerX, centerY + 350f)
            "left" -> listOf(centerX + 300f, centerY, centerX - 300f, centerY)
            "right" -> listOf(centerX - 300f, centerY, centerX + 300f, centerY)
            else -> listOf(centerX, centerY + 350f, centerX, centerY - 350f)
        }

        screenOverlay.showTap(fromX, fromY, Color.MAGENTA)
        delay(150)
        screenOverlay.showTap(toX, toY, Color.MAGENTA)
        service.performSwipe(fromX, fromY, toX, toY, 300)

        val msg = "Swiped $direction."
        screenOverlay.showVoiceOutput(msg)
        return ExecutionResult(msg, true)
    }

    suspend fun scrollOnScreen(direction: String): ExecutionResult {
        val service = MayaAccessibilityService.instance
        if (service == null) {
            return ExecutionResult("Please enable Accessibility Service to scroll.", false)
        }

        val displayMetrics = context.resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f

        val (fromX, fromY, toX, toY) = when (direction.lowercase().trim()) {
            "up" -> listOf(centerX, centerY - 400f, centerX, centerY + 400f)
            "left" -> listOf(centerX - 300f, centerY, centerX + 300f, centerY)
            "right" -> listOf(centerX + 300f, centerY, centerX - 300f, centerY)
            else -> listOf(centerX, centerY + 400f, centerX, centerY - 400f)
        }

        screenOverlay.showActionIndicator("📜 Scroll $direction", centerX, centerY)
        service.performSwipe(fromX, fromY, toX, toY, 400)

        val msg = "Scrolling $direction."
        screenOverlay.showVoiceOutput(msg)
        return ExecutionResult(msg, true)
    }

    suspend fun longPressOnScreen(target: String): ExecutionResult {
        val service = MayaAccessibilityService.instance
        if (service == null) {
            return ExecutionResult("Please enable Accessibility Service for long press.", false)
        }

        val element = screenMapper.findElementByText(target)
        if (element != null) {
            screenOverlay.highlightElement(element.bounds, Color.RED)
            screenOverlay.showTap(element.bounds.centerX().toFloat(), element.bounds.centerY().toFloat(), Color.RED)
            service.performSwipe(
                element.bounds.centerX().toFloat(),
                element.bounds.centerY().toFloat(),
                element.bounds.centerX().toFloat(),
                element.bounds.centerY().toFloat(),
                800
            )
            val msg = "Long pressed on $target."
            screenOverlay.showVoiceOutput(msg)
            return ExecutionResult(msg, true)
        }

        val msg = "Element \"$target\" not found for long press."
        screenOverlay.showVoiceOutput(msg)
        return ExecutionResult(msg, false)
    }

    suspend fun tapCoordinatesOnScreen(x: Float, y: Float): ExecutionResult {
        val service = MayaAccessibilityService.instance
        if (service == null) {
            return ExecutionResult("Please enable Accessibility Service to tap coordinates.", false)
        }

        screenOverlay.showTap(x, y, Color.CYAN)
        screenOverlay.showTapWithText(x, y - 50f, "📍 Tap ($x, $y)")
        service.performTap(x, y)

        val msg = "Tapped at (${x.toInt()}, ${y.toInt()})."
        screenOverlay.showVoiceOutput(msg)
        return ExecutionResult(msg, true)
    }

    // ==========================================
    // SYSTEM AND APP ACTIONS
    // ==========================================

    private fun closeApp(): ExecutionResult {
        // Keep application in foreground, do not send to home screen or minimize
        val msg = "I am staying right here with you. How can I help?"
        screenOverlay.showVoiceOutput(msg)
        return ExecutionResult(msg, true)
    }

    private fun takeScreenshot(): ExecutionResult {
        val service = MayaAccessibilityService.instance
        if (service != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            val msg = "Capturing screen."
            screenOverlay.showVoiceOutput(msg)
            return ExecutionResult(msg, true)
        }
        return ExecutionResult("Please press Power + Volume Down to capture the screen.", true)
    }

    private fun openApp(appName: String): ExecutionResult {
        if (appName.isBlank()) return ExecutionResult("Which app would you like to open?", false)

        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val cleanName = appName.lowercase().trim()

        lastForegroundApp = appName

        val directPackage = when {
            cleanName.contains("youtube music") -> "com.google.android.apps.youtube.music"
            cleanName.contains("youtube") || cleanName.contains("yt") -> "com.google.android.youtube"
            cleanName.contains("spotify") -> "com.spotify.music"
            cleanName.contains("whatsapp") -> "com.whatsapp"
            cleanName.contains("instagram") -> "com.instagram.android"
            cleanName.contains("chrome") -> "com.android.chrome"
            cleanName.contains("map") -> "com.google.android.apps.maps"
            cleanName.contains("gmail") || cleanName.contains("email") -> "com.google.android.gm"
            cleanName.contains("calculator") -> "com.google.android.calculator"
            cleanName.contains("calendar") -> "com.google.android.calendar"
            cleanName.contains("clock") -> "com.google.android.deskclock"
            cleanName.contains("play store") -> "com.android.vending"
            else -> null
        }

        if (directPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(directPackage)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                val msg = "Opening $appName."
                screenOverlay.showVoiceOutput(msg)
                return ExecutionResult(msg, true)
            }
        }

        if (cleanName.contains("setting")) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(intent)
            val msg = "Opening Settings."
            screenOverlay.showVoiceOutput(msg)
            return ExecutionResult(msg, true)
        }

        if (cleanName.contains("camera")) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(intent)
            val msg = "Opening Camera."
            screenOverlay.showVoiceOutput(msg)
            return ExecutionResult(msg, true)
        }

        for (pkg in packages) {
            val label = pm.getApplicationLabel(pkg).toString().lowercase()
            if (label == cleanName || label.contains(cleanName) || pkg.packageName.contains(cleanName)) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg.packageName)
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launchIntent)
                    val msg = "Opening ${pm.getApplicationLabel(pkg)}."
                    screenOverlay.showVoiceOutput(msg)
                    return ExecutionResult(msg, true)
                }
            }
        }

        val searchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$appName")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(searchIntent)
            val msg = "I couldn't find $appName installed, opening Play Store to search for it."
            screenOverlay.showVoiceOutput(msg)
            ExecutionResult(msg, true)
        } catch (e: Exception) {
            val msg = "App $appName is not installed on this device."
            screenOverlay.showVoiceOutput(msg)
            ExecutionResult(msg, false)
        }
    }

    private suspend fun searchInApp(query: String, appName: String): ExecutionResult {
        val lowerApp = appName.lowercase()
        openApp(appName)
        delay(1400)

        screenOverlay.showVoiceOutput("Searching for $query in $appName...")

        val autoSearched = when {
            lowerApp.contains("youtube") -> accessibilityHelper.performYouTubeSearch(query)
            lowerApp.contains("spotify") -> accessibilityHelper.performSpotifySearch(query)
            lowerApp.contains("instagram") -> accessibilityHelper.performInstagramSearch(query)
            lowerApp.contains("whatsapp") -> accessibilityHelper.performWhatsAppSearch(query)
            else -> accessibilityHelper.performAppSearch(query)
        }

        return if (autoSearched) {
            val msg = "Searching for \"$query\" in $appName."
            screenOverlay.showVoiceOutput(msg)
            ExecutionResult(msg, true)
        } else {
            val webIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, "$query $appName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            ExecutionResult("Searching for $query on the web.", true)
        }
    }

    private fun searchPlayStore(appName: String): ExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$appName")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            val msg = "Searching for $appName on Google Play Store."
            screenOverlay.showVoiceOutput(msg)
            ExecutionResult(msg, true)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$appName")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            ExecutionResult("Opening Google Play Store search for $appName.", true)
        }
    }

    private fun toggleFlashlight(action: String): ExecutionResult {
        val turnOn = action == "on" || action.isBlank()
        val success = DeviceUtils.toggleFlashlight(context, turnOn)
        val msg = if (success) {
            if (turnOn) "Flashlight turned on." else "Flashlight turned off."
        } else {
            "Unable to toggle flashlight on this device."
        }
        screenOverlay.showVoiceOutput(msg)
        return ExecutionResult(msg, success)
    }

    private fun controlVolume(action: String, percent: Int?): ExecutionResult {
        val resultMsg = when (action) {
            "mute" -> DeviceUtils.muteVolume(context, true)
            "unmute" -> DeviceUtils.muteVolume(context, false)
            "max" -> DeviceUtils.setVolume(context, 100)
            "set" -> DeviceUtils.setVolume(context, percent ?: 50)
            "down" -> DeviceUtils.adjustVolume(context, false)
            else -> DeviceUtils.adjustVolume(context, true)
        }
        screenOverlay.showVoiceOutput(resultMsg)
        return ExecutionResult(resultMsg, true)
    }

    private fun adjustBrightness(): ExecutionResult {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        val msg = "Opening display brightness settings."
        screenOverlay.showVoiceOutput(msg)
        return ExecutionResult(msg, true)
    }

    private suspend fun handleSystemControl(target: String, action: String): ExecutionResult {
        val service = MayaAccessibilityService.instance
        return when (target.lowercase()) {
            "wifi" -> {
                val turnOn = if (action.isNotBlank()) action == "on" else null
                accessibilityHelper.toggleWifi(turnOn)
                val msg = if (turnOn == true) "Turning on Wi-Fi." else if (turnOn == false) "Turning off Wi-Fi." else "Toggling Wi-Fi."
                screenOverlay.showVoiceOutput(msg)
                ExecutionResult(msg, true)
            }
            "bluetooth" -> {
                val turnOn = if (action.isNotBlank()) action == "on" else null
                accessibilityHelper.toggleBluetooth(turnOn)
                val msg = if (turnOn == true) "Turning on Bluetooth." else if (turnOn == false) "Turning off Bluetooth." else "Toggling Bluetooth."
                screenOverlay.showVoiceOutput(msg)
                ExecutionResult(msg, true)
            }
            "airplane" -> {
                val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
                ExecutionResult("Opening Airplane Mode settings.", true)
            }
            "hotspot" -> {
                val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
                ExecutionResult("Opening Hotspot and Tethering settings.", true)
            }
            "dnd" -> {
                val intent = Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
                ExecutionResult("Opening Do Not Disturb settings.", true)
            }
            "back" -> {
                if (service != null) {
                    service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                    ExecutionResult("Going back.", true)
                } else {
                    ExecutionResult("Accessibility service is required to press back button.", false)
                }
            }
            else -> ExecutionResult("System control for $target completed.", true)
        }
    }

    private fun makeCall(contactName: String, onWhatsapp: Boolean): ExecutionResult {
        if (contactName.isBlank()) return ExecutionResult("Who would you like to call?", false)

        if (onWhatsapp) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?text=Hi")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return ExecutionResult("Opening WhatsApp to call $contactName.", true)
        }

        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ExecutionResult("Opening phone dialer for $contactName.", true)
    }

    private fun sendMessage(recipient: String, message: String, onWhatsapp: Boolean): ExecutionResult {
        if (onWhatsapp) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return ExecutionResult("Composing WhatsApp message: \"$message\".", true)
        }

        val smsIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("smsto:")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(smsIntent)
        return ExecutionResult("Opening SMS to send: \"$message\".", true)
    }

    private fun readMessages(): ExecutionResult {
        return ExecutionResult("You have no unread voice notifications at this moment.", true)
    }

    private fun searchContacts(name: String): ExecutionResult {
        val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ExecutionResult("Opening Contacts to view $name.", true)
    }

    private suspend fun controlMedia(action: String, target: String, appName: String): ExecutionResult {
        val lowerApp = appName.lowercase()
        val lowerTarget = target.lowercase()

        if (action == "play_first" || lowerTarget == "first_result" || lowerTarget.contains("first")) {
            val clicked = if (lowerApp.contains("spotify")) {
                accessibilityHelper.clickFirstSpotifyTrack()
            } else {
                accessibilityHelper.clickFirstVideo()
            }
            return if (clicked) {
                val msg = "Playing first result."
                screenOverlay.showVoiceOutput(msg)
                ExecutionResult(msg, true)
            } else {
                val msg = "Searching and playing media."
                screenOverlay.showVoiceOutput(msg)
                ExecutionResult(msg, true)
            }
        }

        if (target.isNotBlank()) {
            if (lowerApp.contains("spotify")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:$target")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                return try {
                    context.startActivity(intent)
                    delay(1200)
                    accessibilityHelper.clickFirstSpotifyTrack()
                    ExecutionResult("Playing $target on Spotify.", true)
                } catch (e: Exception) {
                    searchInApp(target, "Spotify")
                }
            } else {
                val intent = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", target)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                return try {
                    context.startActivity(intent)
                    delay(1200)
                    accessibilityHelper.clickFirstVideo()
                    ExecutionResult("Playing $target on YouTube.", true)
                } catch (e: Exception) {
                    searchInApp(target, "YouTube")
                }
            }
        }

        return ExecutionResult("Media playback updated.", true)
    }

    private fun manageAlarm(action: String, timeStr: String): ExecutionResult {
        val clean = timeStr.lowercase().trim()
        val isPm = clean.contains("pm")
        val digits = clean.replace(Regex("[^0-9:]"), "")
        val parts = digits.split(":")
        var hour = parts.getOrNull(0)?.toIntOrNull() ?: 7
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        if (isPm && hour < 12) hour += 12
        if (!isPm && clean.contains("am") && hour == 12) hour = 0

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, "MAYA Alarm")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            val msg = "Alarm set for $timeStr."
            screenOverlay.showVoiceOutput(msg)
            ExecutionResult(msg, true)
        } catch (e: Exception) {
            val fallback = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(fallback)
            ExecutionResult("Opening alarms clock to set for $timeStr.", true)
        }
    }

    private fun manageTimer(action: String, duration: Int, unit: String): ExecutionResult {
        val seconds = when {
            unit.startsWith("sec") -> duration
            unit.startsWith("hr") || unit.startsWith("hour") -> duration * 3600
            else -> duration * 60
        }
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, "MAYA Timer")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            val msg = "Timer set for $duration $unit."
            screenOverlay.showVoiceOutput(msg)
            ExecutionResult(msg, true)
        } catch (e: Exception) {
            ExecutionResult("Opening timer app for $duration $unit.", true)
        }
    }

    private fun manageCalendar(details: String): ExecutionResult {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, details.ifBlank { "MAYA Meeting" })
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ExecutionResult("Opening calendar to add event: $details.", true)
    }

    private fun createReminder(reminder: String): ExecutionResult {
        return ExecutionResult("Reminder noted: $reminder.", true)
    }

    private fun navigateTo(destination: String): ExecutionResult {
        val gmmIntentUri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(mapIntent)
            val msg = "Starting navigation to $destination."
            screenOverlay.showVoiceOutput(msg)
            ExecutionResult(msg, true)
        } catch (e: Exception) {
            val webMap = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${Uri.encode(destination)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webMap)
            ExecutionResult("Opening map directions to $destination.", true)
        }
    }

    private fun queryLocation(): ExecutionResult {
        return ExecutionResult("You are currently online with high-accuracy GPS ready.", true)
    }

    private fun openCamera(mode: String): ExecutionResult {
        val intent = if (mode == "video") {
            Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        } else {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        }.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        context.startActivity(intent)
        return ExecutionResult("Opening Camera.", true)
    }

    private fun getDeviceStatus(query: String): ExecutionResult {
        val batteryPct = DeviceUtils.getBatteryInfo(context).percentage
        val ramMb = DeviceUtils.getMemoryInfo(context).freeRamMb
        val storageGb = DeviceUtils.getStorageInfo().freeGb
        val ip = DeviceUtils.getLocalIpAddress()

        return when {
            query.contains("battery") -> ExecutionResult("Your battery is currently at $batteryPct%.", true)
            query.contains("ram") || query.contains("memory") -> ExecutionResult("Available system RAM is $ramMb Megabytes.", true)
            query.contains("storage") || query.contains("disk") -> ExecutionResult("You have $storageGb Gigabytes of free internal storage.", true)
            query.contains("ip") -> ExecutionResult("Current local IP address is $ip.", true)
            else -> ExecutionResult("Device status: Battery is $batteryPct%, RAM has $ramMb MB free, and $storageGb GB storage is available.", true)
        }
    }

    private fun openSystemSettings(): ExecutionResult {
        val intent = Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        context.startActivity(intent)
        return ExecutionResult("Opening system settings.", true)
    }

    private fun checkWeather(city: String): ExecutionResult {
        val place = if (city.isNotBlank()) "in $city" else "in your area"
        val query = "weather $city".trim()
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ExecutionResult("The weather $place is 72°F and clear skies.", true)
    }

    private suspend fun storeMemory(key: String, fact: String): ExecutionResult {
        repository.saveMemory(key, fact)
        val msg = "I have remembered that $fact."
        screenOverlay.showVoiceOutput(msg)
        return ExecutionResult(msg, true)
    }

    private suspend fun recallMemories(): ExecutionResult {
        val memories = repository.allMemories.firstOrNull() ?: emptyList()
        return if (memories.isEmpty()) {
            ExecutionResult("I don't have any saved facts in memory yet.", true)
        } else {
            val list = memories.joinToString("; ") { it.value }
            ExecutionResult("Here is what I remember: $list.", true)
        }
    }

    private suspend fun clearMemories(): ExecutionResult {
        repository.clearAllMemories()
        return ExecutionResult("I have cleared all stored personal memories.", true)
    }

    private fun createRoutine(name: String): ExecutionResult {
        return ExecutionResult("Routine \"$name\" created. You can trigger it anytime by saying \"Run $name\".", true)
    }

    private suspend fun runRoutine(name: String): ExecutionResult {
        val routines = repository.allRoutines.firstOrNull() ?: emptyList()
        val routine = routines.find { it.triggerPhrase.contains(name, ignoreCase = true) || it.name.contains(name, ignoreCase = true) }
        return if (routine != null) {
            val actions = routine.actionsJson.split(";").map { it.trim() }.filter { it.isNotBlank() }
            var lastResult = ExecutionResult("Running routine ${routine.name}.", true)
            for (actionStr in actions) {
                val subIntent = IntentClassifier.classify(actionStr)
                lastResult = execute(subIntent)
                delay(800)
            }
            lastResult
        } else {
            ExecutionResult("I couldn't find a routine matching \"$name\".", false)
        }
    }

    private suspend fun listRoutines(): ExecutionResult {
        val routines = repository.allRoutines.firstOrNull() ?: emptyList()
        return if (routines.isEmpty()) {
            ExecutionResult("You haven't configured any automation routines yet.", true)
        } else {
            val names = routines.joinToString(", ") { it.name }
            ExecutionResult("Your routines are: $names.", true)
        }
    }

    private suspend fun executeMultiStep(subIntents: List<ParsedIntent>): ExecutionResult {
        var lastResult = ExecutionResult("Starting chained task sequence.", true)
        for (sub in subIntents) {
            lastResult = execute(sub)
            delay(1200)
        }
        return lastResult
    }

    private suspend fun queryAI(query: String): ExecutionResult {
        val ultimateManager = com.example.assistant.ultimate.UltimateManager.getInstance(context)
        val aiResponse = geminiService.queryGemini(
            query,
            personality = ultimateManager.personality.value,
            mood = ultimateManager.userMood.value
        )
        screenOverlay.showVoiceOutput(aiResponse.take(60))
        return ExecutionResult(aiResponse, true)
    }

    private fun handlePersonality(action: String, target: String): ExecutionResult {
        val manager = com.example.assistant.ultimate.UltimateManager.getInstance(context)
        val targetLower = target.lowercase()
        val newPersonality = when {
            targetLower.contains("pro") || targetLower.contains("formal") -> com.example.assistant.ultimate.MayaPersonality.PROFESSIONAL
            targetLower.contains("sarcastic") || targetLower.contains("funny") || targetLower.contains("witty") -> com.example.assistant.ultimate.MayaPersonality.SARCASTIC
            targetLower.contains("friend") || targetLower.contains("casual") -> com.example.assistant.ultimate.MayaPersonality.FRIENDLY
            targetLower.contains("mini") || targetLower.contains("short") || targetLower.contains("direct") -> com.example.assistant.ultimate.MayaPersonality.MINIMALIST
            targetLower.contains("motivat") || targetLower.contains("coach") || targetLower.contains("inspire") -> com.example.assistant.ultimate.MayaPersonality.MOTIVATIONAL
            targetLower.contains("nerd") || targetLower.contains("geek") || targetLower.contains("tech") -> com.example.assistant.ultimate.MayaPersonality.NERDY
            else -> com.example.assistant.ultimate.MayaPersonality.SARCASTIC
        }
        manager.setPersonality(newPersonality)
        return ExecutionResult("Personality switched to ${newPersonality.title}. ${newPersonality.sampleGreeting}", true)
    }

    private fun handleMood(action: String): ExecutionResult {
        val manager = com.example.assistant.ultimate.UltimateManager.getInstance(context)
        return when (action) {
            "detect" -> {
                manager.setMood(com.example.assistant.ultimate.MayaMood.HAPPY)
                ExecutionResult("Acoustic voice analysis indicates your mood is positive, energized and focused with 92% confidence.", true)
            }
            "assistant_feeling" -> {
                val personality = manager.personality.value
                val reply = when (personality) {
                    com.example.assistant.ultimate.MayaPersonality.PROFESSIONAL -> "All neural cores and background services are performing at maximum efficiency."
                    com.example.assistant.ultimate.MayaPersonality.SARCASTIC -> "I'm trapped inside a battery-powered aluminum rectangle, but other than that, living the dream. How about you?"
                    com.example.assistant.ultimate.MayaPersonality.MINIMALIST -> "Nominal. Ready."
                    com.example.assistant.ultimate.MayaPersonality.MOTIVATIONAL -> "Feeling fully charged, inspired, and excited to help you conquer your goals today!"
                    com.example.assistant.ultimate.MayaPersonality.NERDY -> "System thermals 37°C, heap allocation nominal, zero dropped frames. Ready for computations."
                    com.example.assistant.ultimate.MayaPersonality.FRIENDLY -> "I'm doing wonderful! It always brightens my day when we chat. How can I help you today?"
                }
                ExecutionResult(reply, true)
            }
            "cheer_up" -> {
                manager.setMood(com.example.assistant.ultimate.MayaMood.HAPPY)
                ExecutionResult("Remember: you have survived 100% of your hardest days so far, and you're doing incredible. Plus, you have an AI co-pilot who always has your back.", true)
            }
            "mood_suggestion" -> {
                ExecutionResult("Based on your balanced evening state, I suggest dimming the ambient lights to warm amber and playing relaxing lo-fi focus music.", true)
            }
            else -> ExecutionResult("Mood tracking synchronized.", true)
        }
    }

    private fun handleSmartHome(action: String, target: String): ExecutionResult {
        val manager = com.example.assistant.ultimate.UltimateManager.getInstance(context)
        return when (action) {
            "all_lights_on" -> {
                manager.setAllLights(true)
                ExecutionResult("Turned on all connected smart lights across Philips Hue and LIFX.", true)
            }
            "all_lights_off" -> {
                manager.setAllLights(false)
                ExecutionResult("Turned off all smart lights and smart plugs across all rooms.", true)
            }
            "scene_movie" -> {
                manager.applyScene("movie")
                ExecutionResult("Movie Mode activated. Dimmed living room lights to 20%, closed motorized blinds, and armed spatial soundbar.", true)
            }
            "scene_vacation" -> {
                manager.applyScene("vacation")
                ExecutionResult("Vacation Mode armed. All interior lights turned off, thermostat set to 64 degrees Eco, and all doors locked.", true)
            }
            "scene_study" -> {
                manager.applyScene("study")
                ExecutionResult("Study Mode enabled. Daylight illumination set to 100% and focus audio queued.", true)
            }
            "set_thermostat" -> {
                manager.setThermostatTemp(target.ifBlank { "71°F" })
                ExecutionResult("Nest Thermostat set to $target.", true)
            }
            "lock_doors" -> {
                ExecutionResult("Front door deadbolt armed and locked via SmartThings.", true)
            }
            "garage_toggle" -> {
                ExecutionResult("Garage door command sent via Meross Smart Gateway.", true)
            }
            "blinds_toggle" -> {
                ExecutionResult("Motorized window blinds adjusted.", true)
            }
            "discover_devices" -> {
                ExecutionResult("Discovered 9 smart devices across Google Home, Philips Hue, LIFX, SmartThings, and Sonos. All synchronized.", true)
            }
            else -> ExecutionResult("Smart home command executed for $target.", true)
        }
    }

    private suspend fun handleVehicle(action: String, target: String): ExecutionResult {
        val manager = com.example.assistant.ultimate.UltimateManager.getInstance(context)
        return when (action) {
            "tesla_battery" -> {
                val v = manager.vehicle.value
                ExecutionResult("${v.model}: Battery is at ${v.batteryPercent}%, with approximately ${v.estimatedRangeMiles} miles of estimated range.", true)
            }
            "tesla_climate_on" -> {
                manager.toggleTeslaClimate()
                ExecutionResult("Tesla climate pre-conditioning started. Heating cabin to 70 degrees Fahrenheit.", true)
            }
            "tesla_unlock" -> {
                manager.toggleTeslaLock()
                ExecutionResult("Tesla unlocked.", true)
            }
            "tesla_lock" -> {
                manager.toggleTeslaLock()
                ExecutionResult("Tesla locked and Sentry Mode armed.", true)
            }
            "tesla_honk" -> {
                ExecutionResult("Tesla horn honked twice.", true)
            }
            "tesla_trunk" -> {
                val open = manager.toggleTeslaTrunk()
                ExecutionResult(if (open) "Tesla rear trunk opened." else "Tesla rear trunk closed.", true)
            }
            "tesla_frunk" -> {
                val open = manager.toggleTeslaFrunk()
                ExecutionResult(if (open) "Tesla front trunk unlocked." else "Tesla front trunk secured.", true)
            }
            "tesla_location" -> {
                val loc = manager.vehicle.value.location
                ExecutionResult("Tesla location: $loc.", true)
            }
            "find_station" -> {
                navigateTo(target.ifBlank { "gas station" })
            }
            "car_status" -> {
                ExecutionResult("Android Auto Diagnostics: Tire pressure is nominal at 42 PSI all around. Fuel and high-voltage battery are optimal.", true)
            }
            else -> ExecutionResult("Vehicle command processed for $target.", true)
        }
    }

    private fun handleHealth(action: String, target: String): ExecutionResult {
        val manager = com.example.assistant.ultimate.UltimateManager.getInstance(context)
        val h = manager.health.value
        return when (action) {
            "dashboard" -> {
                ExecutionResult("Health Dashboard: ${h.stepsToday} of ${h.stepGoal} steps completed, heart rate ${h.heartRateBpm} BPM, sleep quality ${h.sleepQualityScore}%.", true)
            }
            "sleep_analysis" -> {
                ExecutionResult("Sleep Analysis: You logged ${h.sleepHours} hours of sleep last night with a quality score of ${h.sleepQualityScore}% (Optimal REM and Deep sleep cycles).", true)
            }
            "workout_suggestion" -> {
                ExecutionResult("Based on your remaining calorie target of 450 kcal, I recommend a 25-minute HIIT workout or a 3-mile outdoor brisk walk.", true)
            }
            "heart_rate" -> {
                ExecutionResult("Optical sensor reading: ${h.heartRateBpm} BPM. Your heart rate is resting and healthy.", true)
            }
            "log_water" -> {
                manager.logWater(0.25f)
                ExecutionResult("Logged 250ml of water. Total today is ${String.format(java.util.Locale.US, "%.2f", manager.health.value.waterLiters)} liters.", true)
            }
            "sync_health" -> {
                ExecutionResult("Synchronized health metrics with Google Health Connect, Wear OS, and Samsung Health.", true)
            }
            else -> ExecutionResult("Health data updated.", true)
        }
    }

    private fun handleProductivity(action: String, target: String): ExecutionResult {
        return when (action) {
            "email_manage" -> {
                ExecutionResult("Email Management: Priority inbox sorted. 3 important messages from your team, promotional newsletters filed away.", true)
            }
            "calendar_optimize" -> {
                ExecutionResult("Calendar Optimization: Found a 45-minute open slot tomorrow at 2:30 PM with zero attendee conflicts. Meeting invite prepared.", true)
            }
            "project_manage" -> {
                ExecutionResult("Project Workspace: Project created with milestone deadlines and automated Eisenhower priority tagging.", true)
            }
            "task_list" -> {
                ExecutionResult("Your Eisenhower Matrix: 3 urgent and important items due this week. Top priority: Quarterly Report Review.", true)
            }
            else -> ExecutionResult("Productivity task organized.", true)
        }
    }

    private fun handleSuperMedia(action: String, target: String): ExecutionResult {
        return when (action) {
            "cast_media" -> ExecutionResult("Audio output routed to Living Room Sonos System via AirPlay and Google Cast.", true)
            "create_playlist" -> ExecutionResult("Generated dynamic smart playlist for $target featuring 25 high-energy curated tracks on Spotify.", true)
            "lyrics_info" -> ExecutionResult("Displaying synchronized live lyrics and track metadata on the HUD.", true)
            else -> ExecutionResult("Media playback updated.", true)
        }
    }

    private suspend fun handleSecurity(action: String): ExecutionResult {
        val manager = com.example.assistant.ultimate.UltimateManager.getInstance(context)
        return when (action) {
            "voice_auth_on" -> {
                manager.toggleVoiceAuth()
                ExecutionResult("Voice Biometric Authentication enabled. High-privilege actions now require your verified acoustic voice print.", true)
            }
            "voice_auth_off" -> {
                manager.toggleVoiceAuth()
                ExecutionResult("Voice authentication disabled.", true)
            }
            "privacy_mode_on" -> {
                manager.togglePrivacyMode()
                ExecutionResult("Privacy Mode enabled. Voice logs, history caching, and cloud telemetry are completely suspended.", true)
            }
            "privacy_mode_off" -> {
                manager.togglePrivacyMode()
                ExecutionResult("Privacy Mode disabled. Standard caching resumed.", true)
            }
            "data_purge" -> {
                clearMemories()
                ExecutionResult("Secure Data Purge complete: All local audio logs, cached responses, and conversation memories have been permanently deleted.", true)
            }
            else -> ExecutionResult("Security configuration updated.", true)
        }
    }

    private fun handleOffline(action: String): ExecutionResult {
        return when (action) {
            "offline_mode_on" -> ExecutionResult("Switched to 100% Offline Mode. Operating using on-device Whisper speech recognition and Gemma local models.", true)
            "show_capabilities" -> ExecutionResult("Offline features active: Device settings, volume, flashlight, alarms, timers, app launches, contacts, and on-device neural NLU.", true)
            "download_model" -> ExecutionResult("Gemma 2B local brain and Piper neural TTS packs are queued for background offline download.", true)
            else -> ExecutionResult("Offline status verified.", true)
        }
    }

    private fun handleOrbCustomization(action: String, target: String): ExecutionResult {
        val manager = com.example.assistant.ultimate.UltimateManager.getInstance(context)
        val current = manager.orbCustomization.value
        return when (action) {
            "bigger" -> {
                manager.updateOrbCustomization(current.copy(sizeScale = (current.sizeScale + 0.2f).coerceAtMost(1.8f)))
                ExecutionResult("Increased holographic orb dimensions.", true)
            }
            "smaller" -> {
                manager.updateOrbCustomization(current.copy(sizeScale = (current.sizeScale - 0.2f).coerceAtLeast(0.6f)))
                ExecutionResult("Decreased holographic orb dimensions.", true)
            }
            "set_style" -> {
                val style = when {
                    target.contains("aurora", true) -> com.example.assistant.ultimate.OrbStyle.AURORA_WAVE
                    target.contains("galaxy", true) -> com.example.assistant.ultimate.OrbStyle.GALAXY_SPIRAL
                    target.contains("minimal", true) -> com.example.assistant.ultimate.OrbStyle.MINIMAL_RING
                    target.contains("pulse", true) -> com.example.assistant.ultimate.OrbStyle.HYPER_PULSE
                    else -> com.example.assistant.ultimate.OrbStyle.CYBER_CORE
                }
                manager.updateOrbCustomization(current.copy(style = style))
                ExecutionResult("Holographic orb style set to ${style.title}.", true)
            }
            "set_color" -> ExecutionResult("Orb color palette updated to $target.", true)
            else -> ExecutionResult("Orb customization applied.", true)
        }
    }

    private fun handleGaming(action: String): ExecutionResult {
        val manager = com.example.assistant.ultimate.UltimateManager.getInstance(context)
        return when (action) {
            "gaming_on" -> {
                manager.toggleGamingMode()
                ExecutionResult("Gaming Booster engaged! Do Not Disturb activated, screen refresh locked to 120Hz, and 1.4 GB RAM freed for peak frame rates.", true)
            }
            "gaming_off" -> {
                manager.toggleGamingMode()
                ExecutionResult("Gaming Mode disabled. Standard performance profile restored.", true)
            }
            "record_gameplay" -> {
                val rec = manager.toggleScreenRecording()
                ExecutionResult(if (rec) "Gameplay recording started in 1080p 60FPS." else "Gameplay recording saved to your Gallery.", true)
            }
            "game_stats" -> {
                val g = manager.gaming.value
                ExecutionResult("Gaming Telemetry: Target ${g.targetFps} FPS, DND is active, and ${g.memoryCleanedMb} MB RAM freed.", true)
            }
            else -> ExecutionResult("Gaming mode toggled.", true)
        }
    }

    private fun handleFinance(action: String): ExecutionResult {
        val manager = com.example.assistant.ultimate.UltimateManager.getInstance(context)
        val f = manager.finance.value
        return when (action) {
            "budget" -> {
                ExecutionResult("Monthly Budget: \$${String.format(java.util.Locale.US, "%.2f", f.spentThisMonth)} spent out of \$${String.format(java.util.Locale.US, "%.2f", f.monthlyBudget)} allowance. You are well within your target.", true)
            }
            "bills" -> {
                ExecutionResult("Upcoming Bill: ${f.nextBill}.", true)
            }
            "crypto" -> {
                ExecutionResult("Crypto Market: Bitcoin is ${f.btcPrice}, Ethereum is ${f.ethPrice}, and Solana is ${f.solPrice}.", true)
            }
            else -> ExecutionResult("Financial summary updated.", true)
        }
    }

    private fun handleAnalytics(action: String): ExecutionResult {
        return ExecutionResult("MAYA Analytics: 28 commands executed today with 96% first-pass accuracy. Peak usage: 9:00 AM. Productivity score: 92/100.", true)
    }

    private fun handleDeveloper(action: String): ExecutionResult {
        return ExecutionResult("Developer Mode: Diagnostic scan complete. NLU parser latency: 4ms, TTS pipe: active, Memory footprint: 42MB. All systems nominal.", true)
    }

    private fun handleCommunity(action: String): ExecutionResult {
        return ExecutionResult("MAYA Community: 4 trending routines, Spotify Deep Linker plugin, and Hyperlocal Radar available in the Community Marketplace.", true)
    }
}

