package com.example.assistant.core

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.util.Locale

/**
 * Intelligent Multi-step Command Planner for MAYA.
 * Translates single and compound natural language requests into structured,
 * verified execution plans across English, Hindi, Nepali, and Hinglish.
 */
class TaskPlanner(private val context: Context) {

    companion object {
        private const val TAG = "TaskPlanner"

        @Volatile
        private var instance: TaskPlanner? = null

        fun getInstance(context: Context): TaskPlanner {
            return instance ?: synchronized(this) {
                instance ?: TaskPlanner(context.applicationContext).also { instance = it }
            }
        }
    }

    val contextState = TaskContextState()

    /**
     * Inspects a command to determine if it should be executed as an AutomationTask.
     */
    fun isCompoundOrAutomationCommand(command: String): Boolean {
        val clean = command.lowercase().trim()

        // 1. Explicit cancellation or control
        if (clean in listOf("stop", "cancel", "cancel this", "forget this task", "pause", "resume", "retry")) {
            return true
        }

        // 2. Follow-up indicators in English, Hindi, Nepali
        if (clean.startsWith("ab ") || clean.startsWith("now ") || clean.startsWith("ani ") || clean.startsWith("fir ") || clean.startsWith("phir ")) {
            return true
        }

        // 3. Known compound connectors
        val hasConjunction = clean.contains(" and ") ||
                clean.contains(" then ") ||
                clean.contains(" aur ") ||
                clean.contains(" phir ") ||
                clean.contains(" fir ") ||
                clean.contains(" ani ") ||
                clean.contains(" ra ") ||
                clean.contains(", ")

        if (hasConjunction) {
            return true
        }

        // 4. Multi-action patterns like "open X and/to search Y", "play X on Spotify", "search X on YouTube"
        if ((clean.contains("search") || clean.contains("play") || clean.contains("call") || clean.contains("message") || clean.contains("send")) &&
            (clean.contains("youtube") || clean.contains("spotify") || clean.contains("whatsapp") || clean.contains("chrome") || clean.contains("contacts") || clean.contains("instagram"))
        ) {
            return true
        }

        // 5. App launching (we make app launching an AutomationTask so it verifies the launch!)
        if (clean.startsWith("open ") || clean.startsWith("launch ") || clean.contains(" kholo") || clean.contains(" open karo")) {
            return true
        }

        return false
    }

    /**
     * Compiles a user prompt into a fully planned AutomationTask.
     */
    fun planTask(command: String): AutomationTask {
        val original = command.trim()
        val clean = original.lowercase().trim()

        contextState.lastCommand = original

        // Check for Interruption / Control tasks
        if (clean in listOf("stop", "cancel", "cancel this", "forget this task")) {
            return AutomationTask(
                originalCommand = original,
                title = "Cancel Current Task",
                steps = listOf(
                    TaskStep(
                        title = "Cancel Pending Automations",
                        actionType = StepActionType.CUSTOM_ACTION,
                        target = "cancel",
                        expectedState = "Execution stopped"
                    )
                )
            )
        }

        if (clean == "pause") {
            return AutomationTask(
                originalCommand = original,
                title = "Pause Task",
                steps = listOf(
                    TaskStep(
                        title = "Pause Current Task",
                        actionType = StepActionType.CUSTOM_ACTION,
                        target = "pause",
                        expectedState = "Execution paused"
                    )
                )
            )
        }

        if (clean == "resume") {
            return AutomationTask(
                originalCommand = original,
                title = "Resume Task",
                steps = listOf(
                    TaskStep(
                        title = "Resume Paused Task",
                        actionType = StepActionType.CUSTOM_ACTION,
                        target = "resume",
                        expectedState = "Execution resumed"
                    )
                )
            )
        }

        // Check for Follow-Up commands using active context
        val isFollowUp = clean.startsWith("ab ") || clean.startsWith("now ") || clean.startsWith("fir ") || clean.startsWith("phir ")
        val strippedQuery = if (isFollowUp) {
            clean.replace(Regex("^(ab|now|fir|phir)\\s+"), "").trim()
        } else {
            clean
        }

        // Follow-up: Play first/second result
        if (strippedQuery.contains("first") || strippedQuery.contains("pehla") || strippedQuery.contains("second") || strippedQuery.contains("dusra")) {
            val pkg = contextState.lastPackage ?: "com.google.android.youtube"
            val appTitle = contextState.lastActiveApp ?: "YouTube"
            val itemIndex = if (strippedQuery.contains("second") || strippedQuery.contains("dusra")) 2 else 1

            return AutomationTask(
                originalCommand = original,
                title = "$appTitle → Play Result #$itemIndex",
                targetPackage = pkg,
                steps = listOf(
                    TaskStep(
                        title = "Detect Playable Result",
                        actionType = StepActionType.DETECT_ELEMENT,
                        target = "result_item",
                        expectedState = "Playable items visible on screen"
                    ),
                    TaskStep(
                        title = "Tap Result #$itemIndex",
                        actionType = StepActionType.CLICK_FIRST_RESULT,
                        target = itemIndex.toString(),
                        expectedState = "Player screen active"
                    ),
                    TaskStep(
                        title = "Verify Media Playback",
                        actionType = StepActionType.VERIFY_PLAYBACK,
                        target = pkg,
                        expectedState = "Media playing in $appTitle"
                    )
                )
            )
        }

        // Follow-up: Search in currently open app
        if (isFollowUp && (strippedQuery.contains("search") || strippedQuery.contains("khojo"))) {
            val targetQuery = extractQueryAfterKeyword(strippedQuery, listOf("search", "khojo", "karo"))
            val pkg = contextState.lastPackage ?: "com.google.android.youtube"
            val appTitle = contextState.lastActiveApp ?: "YouTube"
            contextState.lastSearchQuery = targetQuery

            return AutomationTask(
                originalCommand = original,
                title = "$appTitle → Search \"$targetQuery\"",
                targetPackage = pkg,
                steps = createSearchInAppSteps(appTitle, pkg, targetQuery)
            )
        }

        // Compound or specific app automated workflows
        // 1. YouTube workflow
        if (clean.contains("youtube") || clean.contains("yt")) {
            val pkg = "com.google.android.youtube"
            contextState.lastActiveApp = "YouTube"
            contextState.lastPackage = pkg

            val hasPlay = clean.contains("play") || clean.contains("chalao") || clean.contains("bajao") || clean.contains("first")
            val hasSearch = clean.contains("search") || clean.contains("dhoondo") || clean.contains("khojo")

            val searchQuery = extractSearchQueryFromAppCommand(original, "youtube")
            contextState.lastSearchQuery = searchQuery

            val steps = mutableListOf<TaskStep>()
            // Step 1 & 2: Launch & Wait
            steps.add(TaskStep(
                title = "Launch YouTube",
                actionType = StepActionType.LAUNCH_APP,
                target = pkg,
                expectedState = "YouTube app launches"
            ))
            steps.add(TaskStep(
                title = "Wait for YouTube Ready",
                actionType = StepActionType.WAIT_FOR_PACKAGE,
                target = pkg,
                expectedState = "YouTube interface is foreground"
            ))

            if (searchQuery.isNotBlank()) {
                // Steps for search
                steps.addAll(createSearchInAppSteps("YouTube", pkg, searchQuery))

                if (hasPlay) {
                    steps.add(TaskStep(
                        title = "Detect First Result",
                        actionType = StepActionType.DETECT_ELEMENT,
                        target = "first_video",
                        expectedState = "Search results visible"
                    ))
                    steps.add(TaskStep(
                        title = "Tap First Video Result",
                        actionType = StepActionType.CLICK_FIRST_RESULT,
                        target = "1",
                        expectedState = "Video playback page opened"
                    ))
                    steps.add(TaskStep(
                        title = "Verify Video Playback",
                        actionType = StepActionType.VERIFY_PLAYBACK,
                        target = pkg,
                        expectedState = "YouTube video playing"
                    ))
                }
            }

            val title = if (searchQuery.isNotBlank()) {
                if (hasPlay) "YouTube → Search \"$searchQuery\" → Play First" else "YouTube → Search \"$searchQuery\""
            } else {
                "Open YouTube"
            }

            return AutomationTask(
                originalCommand = original,
                title = title,
                targetPackage = pkg,
                steps = steps
            )
        }

        // 2. Spotify workflow
        if (clean.contains("spotify")) {
            val pkg = "com.spotify.music"
            contextState.lastActiveApp = "Spotify"
            contextState.lastPackage = pkg

            val songQuery = extractSongFromCommand(original)
            contextState.lastSearchQuery = songQuery

            val steps = mutableListOf<TaskStep>()
            steps.add(TaskStep(
                title = "Launch Spotify",
                actionType = StepActionType.LAUNCH_APP,
                target = pkg,
                expectedState = "Spotify app launches"
            ))
            steps.add(TaskStep(
                title = "Wait for Spotify Ready",
                actionType = StepActionType.WAIT_FOR_PACKAGE,
                target = pkg,
                expectedState = "Spotify interface is foreground"
            ))

            if (songQuery.isNotBlank()) {
                steps.addAll(createSpotifySearchAndPlaySteps(pkg, songQuery))
            }

            return AutomationTask(
                originalCommand = original,
                title = if (songQuery.isNotBlank()) "Spotify → Play \"$songQuery\"" else "Open Spotify",
                targetPackage = pkg,
                steps = steps
            )
        }

        // 3. WhatsApp workflow
        if (clean.contains("whatsapp") || clean.contains("wa")) {
            val pkg = "com.whatsapp"
            contextState.lastActiveApp = "WhatsApp"
            contextState.lastPackage = pkg

            val (recipient, message) = extractWhatsAppDetails(original)
            if (recipient.isNotBlank()) contextState.lastContactName = recipient

            val steps = mutableListOf<TaskStep>()
            steps.add(TaskStep(
                title = "Launch WhatsApp",
                actionType = StepActionType.LAUNCH_APP,
                target = pkg,
                expectedState = "WhatsApp opens"
            ))
            steps.add(TaskStep(
                title = "Wait for WhatsApp Ready",
                actionType = StepActionType.WAIT_FOR_PACKAGE,
                target = pkg,
                expectedState = "WhatsApp home chat screen visible"
            ))

            if (recipient.isNotBlank()) {
                steps.add(TaskStep(
                    title = "Tap Search Icon",
                    actionType = StepActionType.CLICK_TEXT_OR_DESC,
                    target = "Search",
                    parameters = mapOf("desc" to "Search"),
                    expectedState = "Search field active"
                ))
                steps.add(TaskStep(
                    title = "Find Contact \"$recipient\"",
                    actionType = StepActionType.TYPE_TEXT,
                    target = recipient,
                    expectedState = "Contact listed in results"
                ))
                steps.add(TaskStep(
                    title = "Open Chat with $recipient",
                    actionType = StepActionType.CLICK_TEXT_OR_DESC,
                    target = recipient,
                    expectedState = "Conversation with $recipient open"
                ))

                if (message.isNotBlank()) {
                    steps.add(TaskStep(
                        title = "Type Message \"$message\"",
                        actionType = StepActionType.TYPE_TEXT,
                        target = message,
                        expectedState = "Message typed in input bar"
                    ))
                    steps.add(TaskStep(
                        title = "Tap Send Button",
                        actionType = StepActionType.CLICK_TEXT_OR_DESC,
                        target = "Send",
                        parameters = mapOf("desc" to "Send"),
                        expectedState = "Message sent"
                    ))
                    steps.add(TaskStep(
                        title = "Verify Message Delivery",
                        actionType = StepActionType.VERIFY_UI_STATE,
                        target = message,
                        expectedState = "Message visible in thread"
                    ))
                }
            }

            return AutomationTask(
                originalCommand = original,
                title = if (recipient.isNotBlank()) "WhatsApp → $recipient" else "Open WhatsApp",
                targetPackage = pkg,
                steps = steps
            )
        }

        // 4. Chrome / Web Search workflow
        if (clean.contains("chrome") || clean.contains("browser") || clean.contains("google")) {
            val pkg = "com.android.chrome"
            contextState.lastActiveApp = "Chrome"
            contextState.lastPackage = pkg

            val webQuery = extractSearchQueryFromAppCommand(original, "chrome")
            val openFirst = clean.contains("first") || clean.contains("pehla")

            val steps = mutableListOf<TaskStep>()
            steps.add(TaskStep(
                title = "Launch Chrome",
                actionType = StepActionType.LAUNCH_APP,
                target = pkg,
                expectedState = "Chrome browser launches"
            ))
            steps.add(TaskStep(
                title = "Wait for Chrome Ready",
                actionType = StepActionType.WAIT_FOR_PACKAGE,
                target = pkg,
                expectedState = "Address bar is ready"
            ))

            if (webQuery.isNotBlank()) {
                steps.add(TaskStep(
                    title = "Tap Address Bar",
                    actionType = StepActionType.CLICK_ELEMENT,
                    target = "url_bar",
                    parameters = mapOf("desc" to "Search or type URL", "id" to "url_bar"),
                    expectedState = "Search input focused"
                ))
                steps.add(TaskStep(
                    title = "Type Query \"$webQuery\"",
                    actionType = StepActionType.TYPE_TEXT,
                    target = webQuery,
                    expectedState = "Query typed into browser"
                ))
                steps.add(TaskStep(
                    title = "Submit Search",
                    actionType = StepActionType.SUBMIT_SEARCH,
                    target = webQuery,
                    expectedState = "Google search results page loaded"
                ))
                steps.add(TaskStep(
                    title = "Verify Web Results",
                    actionType = StepActionType.VERIFY_UI_STATE,
                    target = "Results",
                    expectedState = "Web page content loaded"
                ))

                if (openFirst) {
                    steps.add(TaskStep(
                        title = "Open First Search Result",
                        actionType = StepActionType.CLICK_FIRST_RESULT,
                        target = "1",
                        expectedState = "Target website opened"
                    ))
                }
            }

            return AutomationTask(
                originalCommand = original,
                title = if (webQuery.isNotBlank()) "Chrome → Search \"$webQuery\"" else "Open Chrome",
                targetPackage = pkg,
                steps = steps
            )
        }

        // 5. Contacts / Phone Calls workflow
        if (clean.contains("contact") || clean.contains("call") || clean.contains("phone") || clean.contains("dial")) {
            val contactName = extractContactName(original)
            if (contactName.isNotBlank()) contextState.lastContactName = contactName

            val steps = listOf(
                TaskStep(
                    title = "Resolve Contact \"$contactName\"",
                    actionType = StepActionType.RESOLVE_CONTACT,
                    target = contactName,
                    expectedState = "Contact details found"
                ),
                TaskStep(
                    title = "Initiate Call Flow",
                    actionType = StepActionType.MAKE_PHONE_CALL,
                    target = contactName,
                    expectedState = "Calling screen visible"
                )
            )

            return AutomationTask(
                originalCommand = original,
                title = "Phone → Call $contactName",
                steps = steps
            )
        }

        // 6. Settings Automation workflow
        if (clean.contains("setting")) {
            val subSetting = when {
                clean.contains("wi-fi") || clean.contains("wifi") -> "wifi"
                clean.contains("bluetooth") -> "bluetooth"
                clean.contains("display") || clean.contains("brightness") -> "display"
                clean.contains("battery") -> "battery"
                clean.contains("app") -> "apps"
                clean.contains("sound") || clean.contains("volume") -> "sound"
                clean.contains("accessibility") -> "accessibility"
                else -> "general"
            }

            val steps = listOf(
                TaskStep(
                    title = "Open Settings: ${subSetting.replaceFirstChar { it.uppercase() }}",
                    actionType = StepActionType.NAVIGATE_SETTINGS,
                    target = subSetting,
                    expectedState = "${subSetting.replaceFirstChar { it.uppercase() }} settings screen displayed"
                ),
                TaskStep(
                    title = "Verify Settings Screen",
                    actionType = StepActionType.VERIFY_UI_STATE,
                    target = subSetting,
                    expectedState = "Settings options active"
                )
            )

            return AutomationTask(
                originalCommand = original,
                title = "Settings → ${subSetting.replaceFirstChar { it.uppercase() }}",
                steps = steps
            )
        }

        // 7. Maps / Navigation workflow
        if (clean.contains("map") || clean.contains("navigate") || clean.contains("directions")) {
            val destination = extractDestination(original)
            val steps = listOf(
                TaskStep(
                    title = "Launch Navigation to \"$destination\"",
                    actionType = StepActionType.START_NAVIGATION,
                    target = destination,
                    expectedState = "Google Maps navigation initiated"
                ),
                TaskStep(
                    title = "Verify Maps Route Screen",
                    actionType = StepActionType.WAIT_FOR_PACKAGE,
                    target = "com.google.android.apps.maps",
                    expectedState = "Maps navigation active"
                )
            )

            return AutomationTask(
                originalCommand = original,
                title = "Maps → Navigate to $destination",
                targetPackage = "com.google.android.apps.maps",
                steps = steps
            )
        }

        // 8. General App Launch + Verification fallback
        val targetApp = extractGenericApp(original)
        val resolvedPackage = resolvePackageName(targetApp)

        if (resolvedPackage != null) {
            contextState.lastActiveApp = targetApp
            contextState.lastPackage = resolvedPackage

            return AutomationTask(
                originalCommand = original,
                title = "Launch & Verify $targetApp",
                targetPackage = resolvedPackage,
                steps = listOf(
                    TaskStep(
                        title = "Launch $targetApp",
                        actionType = StepActionType.LAUNCH_APP,
                        target = resolvedPackage,
                        expectedState = "$targetApp opens"
                    ),
                    TaskStep(
                        title = "Verify Foreground Interface",
                        actionType = StepActionType.WAIT_FOR_PACKAGE,
                        target = resolvedPackage,
                        expectedState = "$targetApp interface is ready"
                    )
                )
            )
        }

        // Fallback single-step task
        return AutomationTask(
            originalCommand = original,
            title = original.take(30),
            steps = listOf(
                TaskStep(
                    title = "Execute: $original",
                    actionType = StepActionType.CUSTOM_ACTION,
                    target = original,
                    expectedState = "Command executed"
                )
            )
        )
    }

    private fun createSearchInAppSteps(appName: String, pkg: String, query: String): List<TaskStep> {
        return listOf(
            TaskStep(
                title = "Detect Search Interface",
                actionType = StepActionType.DETECT_ELEMENT,
                target = "Search",
                parameters = mapOf("desc" to "Search", "id" to "menu_item_search"),
                expectedState = "Search button/icon located"
            ),
            TaskStep(
                title = "Tap Search Field",
                actionType = StepActionType.CLICK_ELEMENT,
                target = "search_button",
                parameters = mapOf("desc" to "Search", "id" to "menu_item_search"),
                expectedState = "Search input field is focused"
            ),
            TaskStep(
                title = "Enter \"$query\"",
                actionType = StepActionType.TYPE_TEXT,
                target = query,
                expectedState = "Query typed in search input"
            ),
            TaskStep(
                title = "Submit Search",
                actionType = StepActionType.SUBMIT_SEARCH,
                target = query,
                expectedState = "Search request submitted"
            ),
            TaskStep(
                title = "Verify Search Results",
                actionType = StepActionType.VERIFY_UI_STATE,
                target = query,
                expectedState = "Search results list visible on screen"
            )
        )
    }

    private fun createSpotifySearchAndPlaySteps(pkg: String, query: String): List<TaskStep> {
        return listOf(
            TaskStep(
                title = "Tap Search Tab",
                actionType = StepActionType.CLICK_TEXT_OR_DESC,
                target = "Search",
                parameters = mapOf("desc" to "Search", "text" to "Search"),
                expectedState = "Search tab opened"
            ),
            TaskStep(
                title = "Type Track \"$query\"",
                actionType = StepActionType.TYPE_TEXT,
                target = query,
                expectedState = "Track name entered"
            ),
            TaskStep(
                title = "Submit Search",
                actionType = StepActionType.SUBMIT_SEARCH,
                target = query,
                expectedState = "Track search results loaded"
            ),
            TaskStep(
                title = "Play Top Song Result",
                actionType = StepActionType.CLICK_FIRST_RESULT,
                target = "1",
                expectedState = "Playback initiated"
            ),
            TaskStep(
                title = "Verify Audio Playback",
                actionType = StepActionType.VERIFY_PLAYBACK,
                target = pkg,
                expectedState = "Music playing in Spotify"
            )
        )
    }

    // ==========================================
    // EXTRACTION HELPERS FOR MULTI-LANGUAGE
    // ==========================================

    private fun extractSearchQueryFromAppCommand(raw: String, appKeyword: String): String {
        var text = raw.lowercase()

        // Strip connector prefixes: "open youtube and search X" -> "X"
        text = text.replace(Regex("(?i)^open\\s+youtube\\s+(and\\s+|then\\s+|to\\s+)?(search\\s+(for\\s+)?|play\\s+)?"), "")
        text = text.replace(Regex("(?i)^launch\\s+youtube\\s+(and\\s+|then\\s+|to\\s+)?(search\\s+(for\\s+)?|play\\s+)?"), "")
        text = text.replace(Regex("(?i)^youtube\\s+(kholo\\s+aur\\s+|open\\s+karo\\s+aur\\s+)?(search\\s+karo\\s+|play\\s+karo\\s+)?"), "")
        text = text.replace(Regex("(?i)^open\\s+chrome\\s+(and\\s+|then\\s+|to\\s+)?(search\\s+(for\\s+)?|google\\s+)?"), "")
        text = text.replace(Regex("(?i)^chrome\\s+(kholo\\s+aur\\s+|open\\s+karo\\s+aur\\s+)?(search\\s+karo\\s+|google\\s+karo\\s+)?"), "")

        // Remove trailing Hindi/Nepali verbs: "Arijit Singh search karo" -> "Arijit Singh"
        text = text.replace(Regex("(?i)\\s+(search\\s+karo|play\\s+karo|kholo|chalao|bajao|search|play|dhoondo)$"), "")

        // Remove trailing "and play the first song"
        text = text.replace(Regex("(?i)\\s+(and|,)?\\s*(play\\s+the\\s+first\\s+(result|video|song)|play\\s+first).*$"), "")

        return text.trim()
    }

    private fun extractSongFromCommand(raw: String): String {
        var text = raw
        text = text.replace(Regex("(?i)^(open\\s+spotify\\s+(and\\s+|then\\s+)?(play\\s+)?|play\\s+|spotify\\s+(mein\\s+|kholo\\s+aur\\s+)?(play\\s+karo\\s+)?)"), "")
        text = text.replace(Regex("(?i)\\s+(on\\s+spotify|in\\s+spotify|play\\s+karo|chalao|bajao)$"), "")
        return text.trim()
    }

    private fun extractWhatsAppDetails(raw: String): Pair<String, String> {
        // e.g. "Open WhatsApp, find Rahul, and send hello"
        // e.g. "WhatsApp kholo aur Rahul ko hello bhejo"
        val clean = raw.trim()

        val recipientMatch = Regex("(?i)(?:find|contact|to|ko)\\s+([A-Za-z0-9_]+)").find(clean)
        val recipient = recipientMatch?.groupValues?.get(1)?.trim() ?: ""

        val messageMatch = Regex("(?i)(?:send|bhejo|message)\\s+(?:to\\s+[A-Za-z0-9_]+\\s*:?\\s*)?(.*)$").find(clean)
        var message = messageMatch?.groupValues?.get(1)?.trim() ?: ""

        if (message.contains("bhejo", ignoreCase = true)) {
            message = message.replace(Regex("(?i)\\s*bhejo.*$"), "").trim()
        }

        return Pair(recipient, message)
    }

    private fun extractContactName(raw: String): String {
        var text = raw
        text = text.replace(Regex("(?i)^(call|phone|dial|open\\s+contacts\\s+and\\s+call|contacts\\s+mein\\s+)"), "")
        text = text.replace(Regex("(?i)\\s+(ko\\s+call\\s+karo|call\\s+karo|ko\\s+phone\\s+karo|her|him)$"), "")
        return text.trim()
    }

    private fun extractDestination(raw: String): String {
        var text = raw
        text = text.replace(Regex("(?i)^(navigate\\s+to|directions\\s+to|open\\s+maps\\s+and\\s+navigate\\s+to|take\\s+me\\s+to)"), "")
        return text.trim()
    }

    private fun extractQueryAfterKeyword(clean: String, keywords: List<String>): String {
        for (kw in keywords) {
            val idx = clean.indexOf(kw)
            if (idx != -1) {
                return clean.substring(idx + kw.length).trim()
            }
        }
        return clean
    }

    private fun extractGenericApp(raw: String): String {
        var text = raw.lowercase().trim()
        text = text.replace(Regex("(?i)^(open\\s+|launch\\s+|kholo\\s+)"), "")
        text = text.replace(Regex("(?i)\\s+(kholo|open\\s+karo|launch|app)$"), "")
        return text.trim()
    }

    private fun resolvePackageName(appName: String): String? {
        val clean = appName.lowercase()
        return when {
            clean.contains("youtube music") -> "com.google.android.apps.youtube.music"
            clean.contains("youtube") || clean == "yt" -> "com.google.android.youtube"
            clean.contains("spotify") -> "com.spotify.music"
            clean.contains("whatsapp") -> "com.whatsapp"
            clean.contains("instagram") -> "com.instagram.android"
            clean.contains("chrome") -> "com.android.chrome"
            clean.contains("map") -> "com.google.android.apps.maps"
            clean.contains("gmail") || clean.contains("email") -> "com.google.android.gm"
            clean.contains("calculator") -> "com.google.android.calculator"
            clean.contains("calendar") -> "com.google.android.calendar"
            clean.contains("clock") -> "com.google.android.deskclock"
            clean.contains("gallery") || clean.contains("photos") -> "com.google.android.apps.photos"
            clean.contains("play store") -> "com.android.vending"
            clean.contains("setting") -> "com.android.settings"
            else -> {
                try {
                    val pm = context.packageManager
                    val pkgs = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    val found = pkgs.firstOrNull {
                        pm.getApplicationLabel(it).toString().lowercase().contains(clean)
                    }
                    found?.packageName
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}
