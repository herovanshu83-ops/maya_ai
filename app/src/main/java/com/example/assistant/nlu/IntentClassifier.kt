package com.example.assistant.nlu

enum class IntentType {
    OPEN_APP,
    CLOSE_APP,
    APP_SEARCH,
    PLAY_STORE_SEARCH,
    SCREEN_MAP,
    FIND_TEXT,
    CLICK_ELEMENT,
    TYPE_TEXT,
    SWIPE,
    SCROLL,
    LONG_PRESS,
    TAP_COORDINATES,
    SYSTEM_CONTROL,
    FLASHLIGHT,
    VOLUME_CONTROL,
    BRIGHTNESS_CONTROL,
    CALL,
    SEND_MESSAGE,
    READ_MESSAGES,
    SEARCH_CONTACT,
    MEDIA_CONTROL,
    ALARM,
    TIMER,
    CALENDAR,
    REMINDER,
    NAVIGATION,
    LOCATION_QUERY,
    CAMERA,
    SCREENSHOT,
    DEVICE_STATUS,
    SETTINGS,
    WEATHER,
    MEMORY_STORE,
    MEMORY_RECALL,
    MEMORY_CLEAR,
    ROUTINE_RUN,
    ROUTINE_CREATE,
    ROUTINE_LIST,
    INTERRUPTION,
    MULTI_STEP,
    OVERLAY_CONTROL,
    BACKGROUND_CONTROL,
    PERSONALITY_CONTROL,
    MOOD_CONTROL,
    SMART_HOME_CONTROL,
    VEHICLE_CONTROL,
    HEALTH_CONTROL,
    PRODUCTIVITY_CONTROL,
    SUPER_MEDIA,
    SECURITY_CONTROL,
    OFFLINE_CONTROL,
    ORB_CUSTOMIZATION,
    GAMING_CONTROL,
    FINANCE_CONTROL,
    ANALYTICS_CONTROL,
    DEVELOPER_CONTROL,
    COMMUNITY_CONTROL,
    AI_QUERY
}

data class ParsedIntent(
    val type: IntentType,
    val rawQuery: String,
    val target: String = "",
    val action: String = "",
    val parameters: Map<String, String> = emptyMap(),
    val subIntents: List<ParsedIntent> = emptyList()
)

object IntentClassifier {

    fun classify(query: String): ParsedIntent {
        val clean = query.trim().lowercase()
        val original = query.trim()

        // 1. Interruption commands
        if (clean in listOf("stop", "cancel", "never mind", "quiet", "shut up", "exit", "quit", "pause", "halt")) {
            return ParsedIntent(type = IntentType.INTERRUPTION, rawQuery = original, action = clean)
        }

        // 2. Chained / Multi-step commands (contains " and ", " then ", or comma separated)
        if ((clean.contains(" and ") || clean.contains(" then ") || clean.contains(", ")) &&
            !clean.startsWith("explain") && !clean.startsWith("translate") && !clean.startsWith("what is") && !clean.startsWith("remember")
        ) {
            val parts = original.split(Regex("(?i)(?:,\\s*and\\s*|,\\s*then\\s*|\\s+and\\s+|\\s+then\\s+|,\\s*)"))
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (parts.size >= 2) {
                val sub = parts.map { classifySubStep(it, parts.first()) }
                return ParsedIntent(
                    type = IntentType.MULTI_STEP,
                    rawQuery = original,
                    subIntents = sub
                )
            }
        }

        // Single step classification
        return classifySingle(original, clean)
    }

    private fun classifySubStep(step: String, initialStep: String): ParsedIntent {
        val clean = step.lowercase().trim()
        val initialClean = initialStep.lowercase().trim()
        val associatedApp = when {
            initialClean.contains("youtube music") -> "YouTube Music"
            initialClean.contains("youtube") || initialClean.contains("yt") -> "YouTube"
            initialClean.contains("spotify") -> "Spotify"
            initialClean.contains("instagram") || initialClean.contains("insta") -> "Instagram"
            initialClean.contains("chrome") || initialClean.contains("google") -> "Chrome"
            initialClean.contains("whatsapp") || initialClean.contains("wa") -> "WhatsApp"
            initialClean.contains("maps") || initialClean.contains("map") -> "Google Maps"
            initialClean.contains("gmail") || initialClean.contains("mail") -> "Gmail"
            else -> ""
        }

        if (clean.startsWith("play first") || clean.startsWith("play the first") || clean.startsWith("click first") || clean == "play") {
            return ParsedIntent(
                type = IntentType.MEDIA_CONTROL,
                rawQuery = step,
                target = "first_result",
                action = "play_first",
                parameters = mapOf("app" to associatedApp)
            )
        }

        if (clean.startsWith("search ") || clean.startsWith("search for ") || clean.startsWith("find ")) {
            val query = clean.replace(Regex("^(search for|search|find)\\s+"), "").trim()
            return ParsedIntent(
                type = IntentType.APP_SEARCH,
                rawQuery = step,
                target = query,
                parameters = mapOf("app" to associatedApp)
            )
        }

        return classifySingle(step, clean)
    }

    private fun classifySingle(original: String, clean: String): ParsedIntent {
        // -1. Overlay & Floating Orb Commands
        if (clean in listOf("show overlay", "open overlay", "enable overlay", "display overlay", "make maya always on top", "always on top", "show floating orb", "show orb", "floating orb")) {
            return ParsedIntent(type = IntentType.OVERLAY_CONTROL, rawQuery = original, action = "show")
        }
        if (clean in listOf("hide overlay", "close overlay", "disable overlay", "hide floating orb", "hide orb", "remove orb")) {
            return ParsedIntent(type = IntentType.OVERLAY_CONTROL, rawQuery = original, action = "hide")
        }
        if (clean in listOf("minimize overlay", "minimize maya", "minimize to orb", "minimize")) {
            return ParsedIntent(type = IntentType.OVERLAY_CONTROL, rawQuery = original, action = "minimize")
        }

        // -2. Background Service & Auto-Start Commands
        if (clean in listOf("keep maya running", "start background service", "run in background", "keep running", "start background")) {
            return ParsedIntent(type = IntentType.BACKGROUND_CONTROL, rawQuery = original, action = "start")
        }
        if (clean in listOf("stop maya", "stop background service", "close maya", "exit maya", "stop service", "quit maya")) {
            return ParsedIntent(type = IntentType.BACKGROUND_CONTROL, rawQuery = original, action = "stop")
        }
        if (clean in listOf("restart maya", "restart background service", "reboot maya")) {
            return ParsedIntent(type = IntentType.BACKGROUND_CONTROL, rawQuery = original, action = "restart")
        }
        if (clean in listOf("check if maya is running", "show background status", "is maya running", "background status", "service status")) {
            return ParsedIntent(type = IntentType.BACKGROUND_CONTROL, rawQuery = original, action = "status")
        }
        if (clean in listOf("enable auto-start", "enable autostart", "start on boot", "auto start on")) {
            return ParsedIntent(type = IntentType.BACKGROUND_CONTROL, rawQuery = original, action = "enable_autostart")
        }
        if (clean in listOf("disable auto-start", "disable autostart", "don't start on boot", "auto start off")) {
            return ParsedIntent(type = IntentType.BACKGROUND_CONTROL, rawQuery = original, action = "disable_autostart")
        }

        // ================= BONUS FEATURE PARSERS =================
        // 1. Personality Customization
        if (clean.contains("change personality to") || clean.contains("set personality to") || clean.contains("switch personality to")) {
            val type = clean.substringAfter("to").trim()
            return ParsedIntent(type = IntentType.PERSONALITY_CONTROL, rawQuery = original, target = type, action = "set")
        }
        if (clean in listOf("make maya funny", "make maya sarcastic", "sarcastic mode", "be sarcastic", "funny mode")) {
            return ParsedIntent(type = IntentType.PERSONALITY_CONTROL, rawQuery = original, target = "sarcastic", action = "set")
        }
        if (clean in listOf("make maya more professional", "make maya professional", "professional mode", "be professional")) {
            return ParsedIntent(type = IntentType.PERSONALITY_CONTROL, rawQuery = original, target = "professional", action = "set")
        }
        if (clean in listOf("make maya friendly", "friendly mode", "be friendly")) {
            return ParsedIntent(type = IntentType.PERSONALITY_CONTROL, rawQuery = original, target = "friendly", action = "set")
        }
        if (clean in listOf("make maya minimalist", "minimalist mode", "be concise")) {
            return ParsedIntent(type = IntentType.PERSONALITY_CONTROL, rawQuery = original, target = "minimalist", action = "set")
        }
        if (clean in listOf("make maya motivational", "motivational mode", "motivate me")) {
            return ParsedIntent(type = IntentType.PERSONALITY_CONTROL, rawQuery = original, target = "motivational", action = "set")
        }
        if (clean in listOf("make maya nerdy", "nerdy mode", "geek mode", "be nerdy")) {
            return ParsedIntent(type = IntentType.PERSONALITY_CONTROL, rawQuery = original, target = "nerdy", action = "set")
        }

        // 2. Mood Detection & Emotional Awareness
        if (clean in listOf("read my mood", "maya read my mood", "detect my mood", "how do i feel", "analyze my mood", "check my mood")) {
            return ParsedIntent(type = IntentType.MOOD_CONTROL, rawQuery = original, action = "detect")
        }
        if (clean in listOf("how are you feeling today?", "how are you feeling today", "how are you feeling", "how do you feel")) {
            return ParsedIntent(type = IntentType.MOOD_CONTROL, rawQuery = original, action = "assistant_feeling")
        }
        if (clean in listOf("cheer me up", "maya cheer me up", "i am sad", "i feel sad", "i am feeling down", "i feel stressed", "i am stressed")) {
            return ParsedIntent(type = IntentType.MOOD_CONTROL, rawQuery = original, action = "cheer_up")
        }
        if (clean.contains("suggest something based on my mood") || clean.contains("customize responses for me")) {
            return ParsedIntent(type = IntentType.MOOD_CONTROL, rawQuery = original, action = "mood_suggestion")
        }

        // 3. Smart Home
        if (clean in listOf("turn on all lights", "turn on lights", "lights on")) {
            return ParsedIntent(type = IntentType.SMART_HOME_CONTROL, rawQuery = original, action = "all_lights_on")
        }
        if (clean in listOf("turn off all lights", "turn off everything", "turn off lights", "lights off")) {
            return ParsedIntent(type = IntentType.SMART_HOME_CONTROL, rawQuery = original, action = "all_lights_off")
        }
        if (clean.contains("movie mode") || clean.contains("turn on movie mode")) {
            return ParsedIntent(type = IntentType.SMART_HOME_CONTROL, rawQuery = original, action = "scene_movie")
        }
        if (clean.contains("vacation mode") || clean.contains("set house to vacation mode")) {
            return ParsedIntent(type = IntentType.SMART_HOME_CONTROL, rawQuery = original, action = "scene_vacation")
        }
        if (clean.contains("study mode") || clean.contains("turn on study mode")) {
            return ParsedIntent(type = IntentType.SMART_HOME_CONTROL, rawQuery = original, action = "scene_study")
        }
        if (clean.startsWith("set thermostat to") || clean.startsWith("set temperature to")) {
            val temp = clean.replace(Regex("^(set thermostat to|set temperature to)\\s+"), "").trim()
            return ParsedIntent(type = IntentType.SMART_HOME_CONTROL, rawQuery = original, target = temp, action = "set_thermostat")
        }
        if (clean in listOf("lock all doors", "lock front door", "lock door")) {
            return ParsedIntent(type = IntentType.SMART_HOME_CONTROL, rawQuery = original, action = "lock_doors")
        }
        if (clean in listOf("open garage", "open garage door", "close garage", "close garage door")) {
            return ParsedIntent(type = IntentType.SMART_HOME_CONTROL, rawQuery = original, action = "garage_toggle")
        }
        if (clean in listOf("close blinds", "open blinds")) {
            return ParsedIntent(type = IntentType.SMART_HOME_CONTROL, rawQuery = original, action = "blinds_toggle")
        }
        if (clean in listOf("discover new devices", "scan for smart devices", "show all connected devices", "show smart devices")) {
            return ParsedIntent(type = IntentType.SMART_HOME_CONTROL, rawQuery = original, action = "discover_devices")
        }

        // 4. Vehicle / Android Auto / Tesla
        if (clean.contains("tesla battery") || clean == "check tesla battery" || clean == "how much battery left in my tesla") {
            return ParsedIntent(type = IntentType.VEHICLE_CONTROL, rawQuery = original, action = "tesla_battery")
        }
        if (clean in listOf("start tesla climate", "turn on tesla climate", "cool down tesla", "precondition tesla battery")) {
            return ParsedIntent(type = IntentType.VEHICLE_CONTROL, rawQuery = original, action = "tesla_climate_on")
        }
        if (clean in listOf("unlock my tesla", "unlock tesla")) {
            return ParsedIntent(type = IntentType.VEHICLE_CONTROL, rawQuery = original, action = "tesla_unlock")
        }
        if (clean in listOf("lock my tesla", "lock tesla")) {
            return ParsedIntent(type = IntentType.VEHICLE_CONTROL, rawQuery = original, action = "tesla_lock")
        }
        if (clean in listOf("honk tesla horn", "honk horn", "honk my tesla")) {
            return ParsedIntent(type = IntentType.VEHICLE_CONTROL, rawQuery = original, action = "tesla_honk")
        }
        if (clean in listOf("open tesla trunk", "open trunk")) {
            return ParsedIntent(type = IntentType.VEHICLE_CONTROL, rawQuery = original, action = "tesla_trunk")
        }
        if (clean in listOf("open tesla frunk", "open frunk")) {
            return ParsedIntent(type = IntentType.VEHICLE_CONTROL, rawQuery = original, action = "tesla_frunk")
        }
        if (clean in listOf("where is my tesla?", "where is my tesla", "find my car", "locate my car")) {
            return ParsedIntent(type = IntentType.VEHICLE_CONTROL, rawQuery = original, action = "tesla_location")
        }
        if (clean in listOf("find gas station", "find gas stations", "nearest gas station")) {
            return ParsedIntent(type = IntentType.VEHICLE_CONTROL, rawQuery = original, target = "gas station", action = "find_station")
        }
        if (clean in listOf("find ev charging station", "find ev charger", "ev charger")) {
            return ParsedIntent(type = IntentType.VEHICLE_CONTROL, rawQuery = original, target = "EV charging station", action = "find_station")
        }
        if (clean in listOf("check tire pressure", "how much fuel left?")) {
            return ParsedIntent(type = IntentType.VEHICLE_CONTROL, rawQuery = original, action = "car_status")
        }

        // 5. Health & Fitness
        if (clean in listOf("track my health", "show my health dashboard", "health dashboard", "show health dashboard")) {
            return ParsedIntent(type = IntentType.HEALTH_CONTROL, rawQuery = original, action = "dashboard")
        }
        if (clean in listOf("analyze my sleep", "how did i sleep", "sleep analysis", "sleep stats")) {
            return ParsedIntent(type = IntentType.HEALTH_CONTROL, rawQuery = original, action = "sleep_analysis")
        }
        if (clean in listOf("suggest a workout", "workout suggestion", "give me a workout")) {
            return ParsedIntent(type = IntentType.HEALTH_CONTROL, rawQuery = original, action = "workout_suggestion")
        }
        if (clean in listOf("check my heart rate", "heart rate", "pulse rate")) {
            return ParsedIntent(type = IntentType.HEALTH_CONTROL, rawQuery = original, action = "heart_rate")
        }
        if (clean in listOf("log water", "drink water", "i drank water", "record water")) {
            return ParsedIntent(type = IntentType.HEALTH_CONTROL, rawQuery = original, action = "log_water")
        }
        if (clean in listOf("sync health data", "sync with health connect", "health connect")) {
            return ParsedIntent(type = IntentType.HEALTH_CONTROL, rawQuery = original, action = "sync_health")
        }

        // 6. Productivity & Task Management
        if (clean in listOf("sort my emails", "reply to all work emails", "show emails from mom", "schedule email for tomorrow")) {
            return ParsedIntent(type = IntentType.PRODUCTIVITY_CONTROL, rawQuery = original, action = "email_manage")
        }
        if (clean.contains("find time for meeting") || clean.contains("schedule team meeting")) {
            return ParsedIntent(type = IntentType.PRODUCTIVITY_CONTROL, rawQuery = original, action = "calendar_optimize")
        }
        if (clean.startsWith("create project:") || clean.startsWith("create project") || clean.contains("track progress on project")) {
            return ParsedIntent(type = IntentType.PRODUCTIVITY_CONTROL, rawQuery = original, action = "project_manage")
        }
        if (clean in listOf("show tasks due this week", "eisenhower matrix", "priority tasks")) {
            return ParsedIntent(type = IntentType.PRODUCTIVITY_CONTROL, rawQuery = original, action = "task_list")
        }

        // 7. Universal Media & Playlists
        if (clean in listOf("play anywhere", "cast to living room", "sync playlist across apps")) {
            return ParsedIntent(type = IntentType.SUPER_MEDIA, rawQuery = original, action = "cast_media")
        }
        if (clean.startsWith("create playlist for") || clean.startsWith("create playlist")) {
            val theme = clean.replace(Regex("^create playlist (for)?\\s*"), "").trim()
            return ParsedIntent(type = IntentType.SUPER_MEDIA, rawQuery = original, target = theme, action = "create_playlist")
        }
        if (clean in listOf("show lyrics for this song", "show lyrics", "lyrics", "who sang this?", "what is this song")) {
            return ParsedIntent(type = IntentType.SUPER_MEDIA, rawQuery = original, action = "lyrics_info")
        }

        // 8. Super Security & Privacy
        if (clean in listOf("enable voice authentication", "enable voice auth")) {
            return ParsedIntent(type = IntentType.SECURITY_CONTROL, rawQuery = original, action = "voice_auth_on")
        }
        if (clean in listOf("disable voice authentication", "disable voice auth")) {
            return ParsedIntent(type = IntentType.SECURITY_CONTROL, rawQuery = original, action = "voice_auth_off")
        }
        if (clean in listOf("enable privacy mode", "privacy mode on", "incognito mode")) {
            return ParsedIntent(type = IntentType.SECURITY_CONTROL, rawQuery = original, action = "privacy_mode_on")
        }
        if (clean in listOf("disable privacy mode", "privacy mode off")) {
            return ParsedIntent(type = IntentType.SECURITY_CONTROL, rawQuery = original, action = "privacy_mode_off")
        }
        if (clean in listOf("delete my voice data", "delete voice data", "wipe voice logs", "export my data")) {
            return ParsedIntent(type = IntentType.SECURITY_CONTROL, rawQuery = original, action = "data_purge")
        }

        // 9. Offline Super Powers
        if (clean in listOf("switch to offline mode", "offline mode on", "go offline")) {
            return ParsedIntent(type = IntentType.OFFLINE_CONTROL, rawQuery = original, action = "offline_mode_on")
        }
        if (clean in listOf("show offline capabilities", "offline status", "check offline model status")) {
            return ParsedIntent(type = IntentType.OFFLINE_CONTROL, rawQuery = original, action = "show_capabilities")
        }
        if (clean.contains("download offline model") || clean.contains("download gemma") || clean.contains("download offline voice pack")) {
            return ParsedIntent(type = IntentType.OFFLINE_CONTROL, rawQuery = original, action = "download_model")
        }

        // 10. Orb Customization
        if (clean.startsWith("change orb color to") || clean.startsWith("set orb color to")) {
            val color = clean.substringAfter("to").trim()
            return ParsedIntent(type = IntentType.ORB_CUSTOMIZATION, rawQuery = original, target = color, action = "set_color")
        }
        if (clean in listOf("make orb bigger", "increase orb size", "bigger orb")) {
            return ParsedIntent(type = IntentType.ORB_CUSTOMIZATION, rawQuery = original, action = "bigger")
        }
        if (clean in listOf("make orb smaller", "decrease orb size", "smaller orb")) {
            return ParsedIntent(type = IntentType.ORB_CUSTOMIZATION, rawQuery = original, action = "smaller")
        }
        if (clean.contains("set orb to") && clean.contains("style")) {
            val style = clean.replace(Regex("^(set orb to|change orb to)\\s+"), "").replace("style", "").trim()
            return ParsedIntent(type = IntentType.ORB_CUSTOMIZATION, rawQuery = original, target = style, action = "set_style")
        }

        // 11. Gaming Mode
        if (clean in listOf("enable gaming mode", "gaming mode on", "start gaming", "optimize phone for gaming", "game booster")) {
            return ParsedIntent(type = IntentType.GAMING_CONTROL, rawQuery = original, action = "gaming_on")
        }
        if (clean in listOf("disable gaming mode", "gaming mode off", "stop gaming")) {
            return ParsedIntent(type = IntentType.GAMING_CONTROL, rawQuery = original, action = "gaming_off")
        }
        if (clean in listOf("start recording gameplay", "record gameplay", "screen record game")) {
            return ParsedIntent(type = IntentType.GAMING_CONTROL, rawQuery = original, action = "record_gameplay")
        }
        if (clean in listOf("show game stats", "gaming stats", "fps counter")) {
            return ParsedIntent(type = IntentType.GAMING_CONTROL, rawQuery = original, action = "game_stats")
        }

        // 12. Financial Assistant
        if (clean in listOf("track my expenses", "show monthly budget", "monthly budget", "spending summary")) {
            return ParsedIntent(type = IntentType.FINANCE_CONTROL, rawQuery = original, action = "budget")
        }
        if (clean in listOf("upcoming bills", "bills due", "check bills")) {
            return ParsedIntent(type = IntentType.FINANCE_CONTROL, rawQuery = original, action = "bills")
        }
        if (clean in listOf("crypto market update", "crypto update", "bitcoin price", "check crypto", "crypto prices")) {
            return ParsedIntent(type = IntentType.FINANCE_CONTROL, rawQuery = original, action = "crypto")
        }

        // 13. Analytics & Insights
        if (clean in listOf("show usage summary", "show most used commands", "show productivity score", "show today's stats", "give me insights")) {
            return ParsedIntent(type = IntentType.ANALYTICS_CONTROL, rawQuery = original, action = "analytics_summary")
        }

        // 14. Developer Mode
        if (clean in listOf("enable developer mode", "developer mode on", "dev mode")) {
            return ParsedIntent(type = IntentType.DEVELOPER_CONTROL, rawQuery = original, action = "dev_on")
        }
        if (clean in listOf("run debug test", "show command log", "test api", "run performance test")) {
            return ParsedIntent(type = IntentType.DEVELOPER_CONTROL, rawQuery = original, action = "debug_test")
        }

        // 15. Community Hub
        if (clean in listOf("browse plugins", "browse community", "share my routine", "download trending routine", "create new skill", "top community commands")) {
            return ParsedIntent(type = IntentType.COMMUNITY_CONTROL, rawQuery = original, action = "community_hub")
        }

        // 0. Screen Mapping & Direct Screen Actions
        if (clean.contains("show me the screen") || clean.contains("map screen") || clean.contains("map the screen") ||
            clean.contains("show screen map") || clean.contains("show clickable elements") || clean.contains("what's on my screen") ||
            clean.contains("read my screen") || clean.contains("scan screen")
        ) {
            val showClickableOnly = clean.contains("clickable")
            return ParsedIntent(
                type = IntentType.SCREEN_MAP,
                rawQuery = original,
                action = if (showClickableOnly) "clickable" else "all"
            )
        }

        if (clean.startsWith("find ") && (clean.contains("on screen") || clean.contains("in screen"))) {
            val target = clean.replace(Regex("^(find|locate)\\s+"), "")
                .replace(Regex("\\s+on screen$|\\s+in screen$"), "")
                .trim()
            return ParsedIntent(
                type = IntentType.FIND_TEXT,
                rawQuery = original,
                target = target
            )
        }

        // Tap at coordinates (e.g., "tap at 500 300", "click at 200, 400")
        val coordMatch = Regex("(?i)(?:tap|click)\\s+at\\s+(\\d+)[\\s,]+(\\d+)").find(clean)
        if (coordMatch != null) {
            val x = coordMatch.groupValues[1]
            val y = coordMatch.groupValues[2]
            return ParsedIntent(
                type = IntentType.TAP_COORDINATES,
                rawQuery = original,
                parameters = mapOf("x" to x, "y" to y)
            )
        }

        // Long press
        if (clean.startsWith("long press") || clean.startsWith("long click") || clean.startsWith("hold ")) {
            val target = original.replace(Regex("(?i)^(long press on|long press|long click on|long click|hold on|hold)\\s+"), "").trim()
            return ParsedIntent(
                type = IntentType.LONG_PRESS,
                rawQuery = original,
                target = target
            )
        }

        // Swipe & Scroll
        if (clean.startsWith("swipe ")) {
            val dir = clean.replace("swipe ", "").trim()
            return ParsedIntent(type = IntentType.SWIPE, rawQuery = original, target = dir)
        }
        if (clean.startsWith("scroll ") || clean in listOf("scroll down", "scroll up", "scroll left", "scroll right")) {
            val dir = clean.replace("scroll ", "").trim().ifBlank { "down" }
            return ParsedIntent(type = IntentType.SCROLL, rawQuery = original, target = dir)
        }

        // Type text (e.g., "type cats in search", "type hello world")
        if (clean.startsWith("type ") || clean.startsWith("enter text ")) {
            val textToType = original.replace(Regex("(?i)^(type|enter text)\\s+"), "")
                .replace(Regex("(?i)\\s+in search$|\\s+in search box$"), "")
                .trim()
            return ParsedIntent(
                type = IntentType.TYPE_TEXT,
                rawQuery = original,
                target = textToType,
                parameters = mapOf("text" to textToType)
            )
        }

        // Click / Tap Element (e.g., "click search", "click first result", "click the login button", "tap submit")
        if (clean.startsWith("click ") || clean.startsWith("tap ") || clean.startsWith("press ")) {
            val element = original.replace(Regex("(?i)^(click on the|click on|click the|click|tap on the|tap on|tap the|tap|press on the|press the|press)\\s+"), "").trim()
            if (!clean.startsWith("tap at ")) {
                return ParsedIntent(
                    type = IntentType.CLICK_ELEMENT,
                    rawQuery = original,
                    target = element
                )
            }
        }

        // 1. In-App Searches (e.g., "Search for Lofi in YouTube", "Search cats on Instagram")
        val inAppMatch = Regex("(?i)(?:search for|search|find)\\s+(.+?)\\s+(?:on|in|using)\\s+([a-zA-Z0-9\\s]+)").find(original)
        if (inAppMatch != null) {
            val queryParam = inAppMatch.groupValues[1].trim()
            val appParam = inAppMatch.groupValues[2].trim()
            return ParsedIntent(
                type = IntentType.APP_SEARCH,
                rawQuery = original,
                target = queryParam,
                parameters = mapOf("app" to appParam)
            )
        }

        // 2. Play Store Install/Search (e.g., "Install WhatsApp", "Download Spotify", "Search Minecraft on Play Store")
        if (clean.startsWith("install ") || clean.startsWith("download ") || clean.contains("play store")) {
            val appName = original.replace(Regex("(?i)^(install|download|get|search for|search)\\s+"), "")
                .replace(Regex("(?i)\\s+(on|from|in)\\s+(play store|google play)$"), "")
                .trim()
            return ParsedIntent(
                type = IntentType.PLAY_STORE_SEARCH,
                rawQuery = original,
                target = appName
            )
        }

        // 3. Hardware / System Toggles (Wi-Fi, Bluetooth, Airplane, Hotspot, DND, Flashlight, Volume, Brightness)
        if (clean.contains("wifi") || clean.contains("wi-fi") || clean.contains("internet")) {
            val action = if (clean.contains("off") || clean.contains("disable")) "off" else "on"
            return ParsedIntent(IntentType.SYSTEM_CONTROL, original, target = "wifi", action = action)
        }
        if (clean.contains("bluetooth")) {
            val action = if (clean.contains("off") || clean.contains("disable")) "off" else "on"
            return ParsedIntent(IntentType.SYSTEM_CONTROL, original, target = "bluetooth", action = action)
        }
        if (clean.contains("flashlight") || clean.contains("torch")) {
            val action = if (clean.contains("off") || clean.contains("disable")) "off" else "on"
            return ParsedIntent(IntentType.FLASHLIGHT, original, action = action)
        }
        if (clean.contains("airplane mode") || clean.contains("flight mode")) {
            return ParsedIntent(IntentType.SYSTEM_CONTROL, original, target = "airplane")
        }
        if (clean.contains("hotspot")) {
            return ParsedIntent(IntentType.SYSTEM_CONTROL, original, target = "hotspot")
        }
        if (clean.contains("do not disturb") || clean.contains("dnd")) {
            return ParsedIntent(IntentType.SYSTEM_CONTROL, original, target = "dnd")
        }
        if (clean.contains("screenshot") || clean.contains("capture screen")) {
            return ParsedIntent(IntentType.SCREENSHOT, original)
        }
        if (clean.contains("go home") || clean == "home") {
            return ParsedIntent(IntentType.CLOSE_APP, original, action = "home")
        }
        if (clean.contains("go back") || clean == "back") {
            return ParsedIntent(IntentType.SYSTEM_CONTROL, original, target = "back")
        }

        // 4. Volume Control
        if (clean.contains("volume") || clean.contains("mute") || clean.contains("unmute")) {
            val percentMatch = Regex("(\\d+)\\s*%").find(clean)
            val percent = percentMatch?.groupValues?.get(1)?.toIntOrNull()
            val action = when {
                clean.contains("mute") -> "mute"
                clean.contains("unmute") -> "unmute"
                clean.contains("max") || clean.contains("100") -> "max"
                clean.contains("up") || clean.contains("increase") || clean.contains("higher") -> "up"
                clean.contains("down") || clean.contains("decrease") || clean.contains("lower") -> "down"
                percent != null -> "set"
                else -> "up"
            }
            return ParsedIntent(
                type = IntentType.VOLUME_CONTROL,
                rawQuery = original,
                action = action,
                parameters = if (percent != null) mapOf("percent" to percent.toString()) else emptyMap()
            )
        }

        // 5. Brightness
        if (clean.contains("brightness")) {
            return ParsedIntent(IntentType.BRIGHTNESS_CONTROL, original)
        }

        // 6. Alarms & Timers
        if (clean.contains("alarm")) {
            val timeMatch = Regex("(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm|a\\.m\\.|p\\.m\\.))").find(clean)
            val cancel = clean.contains("cancel") || clean.contains("delete") || clean.contains("turn off")
            val show = clean.contains("show") || clean.contains("list")
            return ParsedIntent(
                type = IntentType.ALARM,
                rawQuery = original,
                action = if (cancel) "cancel" else if (show) "show" else "set",
                parameters = mapOf("time" to (timeMatch?.groupValues?.get(1) ?: "7:00 AM"))
            )
        }
        if (clean.contains("timer")) {
            val cancel = clean.contains("cancel") || clean.contains("stop")
            val numMatch = Regex("(\\d+)\\s*(minute|min|second|sec|hour|hr)").find(clean)
            val duration = numMatch?.groupValues?.get(1)?.toIntOrNull() ?: 5
            val unit = numMatch?.groupValues?.get(2) ?: "minute"
            return ParsedIntent(
                type = IntentType.TIMER,
                rawQuery = original,
                action = if (cancel) "cancel" else "set",
                parameters = mapOf("duration" to duration.toString(), "unit" to unit)
            )
        }

        // 7. Phone Calls & Communication
        if (clean.startsWith("call ") || clean.startsWith("dial ") || clean.contains("phone call")) {
            val contact = original.replace(Regex("(?i)^(call|dial|phone call)\\s+"), "")
                .replace(Regex("(?i)\\s+on speaker$"), "")
                .replace(Regex("(?i)\\s+on whatsapp$"), "")
                .trim()
            val onWhatsapp = clean.contains("whatsapp")
            return ParsedIntent(
                type = IntentType.CALL,
                rawQuery = original,
                target = contact,
                parameters = mapOf("whatsapp" to onWhatsapp.toString())
            )
        }

        // 8. SMS & Messaging
        if (clean.startsWith("send message") || clean.startsWith("text ") || clean.startsWith("send sms") || clean.startsWith("send whatsapp") || clean.startsWith("message ")) {
            val toMatch = Regex("(?i)(?:send\\s+(?:message|sms|whatsapp)\\s+to|send\\s+(?:message|sms|whatsapp)|message\\s+to|message|text\\s+to|text)\\s+([a-zA-Z0-9]+)(?:\\s+(?:saying|that|text)\\s+|:\\s*)?(.*)$").find(original)
            val recipient = toMatch?.groupValues?.get(1)?.trim() ?: "Contact"
            val message = toMatch?.groupValues?.get(2)?.trim()?.ifBlank { "Hello" } ?: "Hello"
            return ParsedIntent(
                type = IntentType.SEND_MESSAGE,
                rawQuery = original,
                target = recipient,
                parameters = mapOf("message" to message, "whatsapp" to clean.contains("whatsapp").toString())
            )
        }

        if (clean.contains("read my message") || clean.contains("read messages") || clean.contains("read sms") || clean.contains("read notifications")) {
            return ParsedIntent(IntentType.READ_MESSAGES, original)
        }

        if (clean.startsWith("show contacts") || clean.startsWith("find contact") || clean.startsWith("show my contacts")) {
            val name = clean.replace("show contacts", "").replace("find contact", "").replace("show my contacts", "").trim()
            return ParsedIntent(IntentType.SEARCH_CONTACT, original, target = name)
        }

        // 9. Media & Music
        if (clean.startsWith("play ") || clean == "play" || clean == "pause" || clean == "resume" || clean == "next song" || clean == "previous song") {
            val song = clean.replace(Regex("^(play|search)\\s+"), "")
                .replace(" on spotify", "")
                .replace(" on youtube", "")
                .trim()
            val service = if (clean.contains("spotify")) "spotify" else if (clean.contains("youtube")) "youtube" else "default"
            return ParsedIntent(
                type = IntentType.MEDIA_CONTROL,
                rawQuery = original,
                target = song,
                action = if (clean == "pause") "pause" else if (clean == "next song" || clean == "skip") "next" else if (clean == "previous song") "previous" else "play",
                parameters = mapOf("service" to service, "app" to if (clean.contains("spotify")) "Spotify" else "YouTube")
            )
        }

        // 10. Navigation & Places
        if (clean.startsWith("navigate to") || clean.startsWith("directions to") || clean.startsWith("take me to") || clean.contains("near me") || clean.contains("nearby")) {
            val destination = clean.replace(Regex("^(navigate to|directions to|take me to|find)\\s+"), "").trim()
            return ParsedIntent(
                type = IntentType.NAVIGATION,
                rawQuery = original,
                target = destination
            )
        }
        if (clean.contains("where am i") || clean.contains("what's my location") || clean.contains("current location")) {
            return ParsedIntent(IntentType.LOCATION_QUERY, original)
        }

        // 11. Camera & Photos
        if (clean.contains("take a photo") || clean.contains("take a picture") || clean.contains("open camera") || clean.contains("selfie")) {
            val isFront = clean.contains("selfie") || clean.contains("front")
            return ParsedIntent(IntentType.CAMERA, original, action = if (isFront) "selfie" else "photo")
        }
        if (clean.contains("record video") || clean.contains("record a video")) {
            return ParsedIntent(IntentType.CAMERA, original, action = "video")
        }
        if (clean.contains("gallery") || clean.contains("photos") || clean.contains("show my pictures")) {
            return ParsedIntent(IntentType.CAMERA, original, action = "gallery")
        }

        // 12. Calendar & Events
        if (clean.startsWith("create event") || clean.startsWith("add event") || clean.startsWith("schedule meeting") || clean.contains("calendar")) {
            return ParsedIntent(IntentType.CALENDAR, original, target = original)
        }

        // 13. Device Status
        if (clean.contains("battery") || clean.contains("ram") || clean.contains("storage") || clean.contains("cpu") || clean.contains("android version") || clean.contains("device info") || clean.contains("ip address")) {
            return ParsedIntent(IntentType.DEVICE_STATUS, original, target = clean)
        }

        // 14. Weather
        if (clean.contains("weather") || clean.contains("temperature") || clean.contains("will it rain") || clean.contains("forecast")) {
            val cityMatch = Regex("in\\s+([a-zA-Z\\s]+)").find(clean)
            val city = cityMatch?.groupValues?.get(1)?.trim() ?: ""
            return ParsedIntent(IntentType.WEATHER, original, target = city)
        }

        // 15. Memory System
        if (clean.startsWith("remember that") || clean.startsWith("remember my") || clean.startsWith("remember ")) {
            val fact = original.replace(Regex("(?i)^remember(\\s+that|\\s+my)?\\s+"), "").trim()
            val key = if (fact.contains(" is ")) fact.substringBefore(" is ") else fact.take(20)
            return ParsedIntent(IntentType.MEMORY_STORE, original, target = key, parameters = mapOf("fact" to fact))
        }
        if (clean.contains("what do you remember") || clean.contains("show memory") || clean.contains("show all memories") || clean.contains("show preferences")) {
            return ParsedIntent(IntentType.MEMORY_RECALL, original)
        }
        if (clean.contains("forget everything") || clean.contains("clear memory") || clean.contains("clear all memory")) {
            return ParsedIntent(IntentType.MEMORY_CLEAR, original)
        }

        // 16. Routines Automation
        if (clean.startsWith("create routine") || clean.startsWith("add routine")) {
            val routineName = original.replace(Regex("(?i)^create routine\\s+"), "").replace(Regex("(?i)^add routine\\s+"), "").trim()
            return ParsedIntent(IntentType.ROUTINE_CREATE, original, target = routineName)
        }
        if (clean.startsWith("run ") || clean.startsWith("activate ") || clean.startsWith("start routine ")) {
            val routineName = clean.replace(Regex("^(run|activate|start routine)\\s+"), "").trim()
            return ParsedIntent(IntentType.ROUTINE_RUN, original, target = routineName)
        }
        if (clean.contains("show routines") || clean.contains("show my routines") || clean.contains("list routines")) {
            return ParsedIntent(IntentType.ROUTINE_LIST, original)
        }

        // 17. App Launching (e.g., "Open YouTube", "Launch Spotify", "Start WhatsApp", "Go to Chrome")
        if (clean.startsWith("open ") || clean.startsWith("launch ") || clean.startsWith("start ") || clean.startsWith("go to ")) {
            val appName = original.replace(Regex("(?i)^(open|launch|start|go to)\\s+"), "").trim()
            return ParsedIntent(IntentType.OPEN_APP, original, target = appName)
        }

        // 18. Settings
        if (clean == "settings" || clean == "open settings" || clean.contains("system settings")) {
            return ParsedIntent(IntentType.SETTINGS, original)
        }

        // 19. General Knowledge & AI
        return ParsedIntent(IntentType.AI_QUERY, original)
    }
}
