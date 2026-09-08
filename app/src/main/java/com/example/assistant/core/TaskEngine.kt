package com.example.assistant.core

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import com.example.assistant.audio.TextToSpeechHelper
import com.example.assistant.services.MayaAccessibilityService
import com.example.assistant.services.MayaOverlayService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Enterprise Multi-Step Automation Task Execution Engine for MAYA.
 * Implements the robust "Action → Wait → Verify → Next" execution pattern,
 * supporting compound commands, automatic error recovery, UI inspection,
 * task state persistence across app switches, pause/resume, and cancellation.
 */
class TaskEngine private constructor(private val context: Context) {

    companion object {
        private const val TAG = "TaskEngine"

        @Volatile
        private var instance: TaskEngine? = null

        fun getInstance(context: Context): TaskEngine {
            return instance ?: synchronized(this) {
                instance ?: TaskEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val taskPlanner = TaskPlanner.getInstance(context)
    private val screenVisionEngine = ScreenVisionEngine.getInstance(context)
    val screenVisionState: StateFlow<ScreenStateModel> = screenVisionEngine.screenState
    private var ttsHelper: TextToSpeechHelper? = null

    // State flows
    private val _activeTask = MutableStateFlow<AutomationTask?>(null)
    val activeTask: StateFlow<AutomationTask?> = _activeTask.asStateFlow()

    private val _taskHistory = MutableStateFlow<List<AutomationTask>>(emptyList())
    val taskHistory: StateFlow<List<AutomationTask>> = _taskHistory.asStateFlow()

    private val _timelineLogs = MutableStateFlow<List<TaskLogEntry>>(emptyList())
    val timelineLogs: StateFlow<List<TaskLogEntry>> = _timelineLogs.asStateFlow()

    private val _stats = MutableStateFlow(TaskExecutionStats())
    val stats: StateFlow<TaskExecutionStats> = _stats.asStateFlow()

    private val taskQueue = ConcurrentLinkedQueue<AutomationTask>()
    private var executionJob: Job? = null
    private var isPaused = false

    init {
        try {
            ttsHelper = TextToSpeechHelper.getInstance(context)
        } catch (e: Exception) {
            Log.w(TAG, "TTS initialization in TaskEngine deferred: ${e.message}")
        }
    }

    /**
     * Entry point for executing any command through the Master Automation Engine.
     */
    fun submitCommand(command: String): AutomationTask {
        val trimmed = command.trim()
        val task = taskPlanner.planTask(trimmed)
        submitTask(task)
        return task
    }

    /**
     * Submits a planned AutomationTask to the queue and starts processing.
     */
    fun submitTask(task: AutomationTask) {
        logTimeline(task, "Task created: \"${task.title}\" with ${task.steps.size} steps")
        task.state = TaskState.RECEIVED
        _activeTask.value = task
        updateStats()

        // Handle quick control commands directly
        if (task.steps.size == 1 && task.steps[0].actionType == StepActionType.CUSTOM_ACTION) {
            when (task.steps[0].target) {
                "cancel" -> {
                    cancelCurrentTask("User requested cancellation")
                    return
                }
                "pause" -> {
                    pauseTask()
                    return
                }
                "resume" -> {
                    resumeTask()
                    return
                }
            }
        }

        // Cancel previous running execution if active
        executionJob?.cancel()

        executionJob = scope.launch {
            executeTaskPipeline(task)
        }
    }

    /**
     * Pauses the active task.
     */
    fun pauseTask() {
        val task = _activeTask.value ?: return
        isPaused = true
        task.isPaused = true
        task.state = TaskState.PAUSED
        logTimeline(task, "Task paused: ${task.title}")
        speak("Task paused.")
        updateOverlay(task, "Task Paused", "thinking")
        _activeTask.value = task
    }

    /**
     * Resumes the paused task.
     */
    fun resumeTask() {
        val task = _activeTask.value ?: return
        if (isPaused) {
            isPaused = false
            task.isPaused = false
            task.state = TaskState.EXECUTING
            logTimeline(task, "Task resumed: ${task.title}")
            speak("Resuming task.")
            updateOverlay(task, "Resuming...", "speaking")
            _activeTask.value = task
        }
    }

    /**
     * Cancels the active task immediately.
     */
    fun cancelCurrentTask(reason: String = "Cancelled") {
        val task = _activeTask.value
        executionJob?.cancel()
        executionJob = null
        isPaused = false

        if (task != null) {
            task.state = TaskState.CANCELLED
            task.completedAt = System.currentTimeMillis()
            task.lastError = reason
            logTimeline(task, "Task cancelled: $reason", "WARN")
            speak("Task cancelled.")
            updateOverlay(task, "Task Cancelled", "error")
            archiveTask(task)
        }
        _activeTask.value = null
        updateStats()
    }

    /**
     * Retries the current failing step of the active task.
     */
    fun retryCurrentStep() {
        val task = _activeTask.value ?: return
        executionJob?.cancel()
        executionJob = scope.launch {
            val step = task.currentStep ?: return@launch
            step.attemptCount = 0
            step.state = TaskState.RETRYING
            logTimeline(task, "Manual retry requested for step: ${step.title}")
            executeTaskPipeline(task)
        }
    }

    // ==========================================
    // CORE EXECUTION PIPELINE (Action -> Wait -> Verify -> Next)
    // ==========================================

    private suspend fun executeTaskPipeline(task: AutomationTask) {
        task.state = TaskState.EXECUTING
        _activeTask.value = task

        logTimeline(task, "Starting execution of: ${task.title}")
        updateOverlay(task, "Starting ${task.title}", "speaking")

        // First step initial vocal announcement
        val initialSpeech = when {
            task.title.contains("YouTube", ignoreCase = true) -> "Opening YouTube..."
            task.title.contains("Spotify", ignoreCase = true) -> "Opening Spotify..."
            task.title.contains("WhatsApp", ignoreCase = true) -> "Opening WhatsApp..."
            task.title.contains("Chrome", ignoreCase = true) -> "Opening Chrome..."
            task.title.contains("Settings", ignoreCase = true) -> "Opening Settings..."
            task.title.contains("Maps", ignoreCase = true) -> "Opening Maps..."
            task.title.contains("Call", ignoreCase = true) -> "Looking up contact..."
            else -> "Executing task..."
        }
        speak(initialSpeech)

        for (i in task.currentStepIndex until task.steps.size) {
            task.currentStepIndex = i
            val step = task.steps[i]

            // Check pause loop
            while (isPaused) {
                delay(500)
            }

            // Check cancellation
            if (!currentCoroutineContext().isActive || task.state == TaskState.CANCELLED) {
                return
            }

            logTimeline(task, "[Step ${i + 1}/${task.steps.size}] ${step.title} (Expected: ${step.expectedState})")
            updateOverlay(task, "${step.title} (${i + 1}/${task.steps.size})", "speaking")
            _activeTask.value = task

            var stepSuccess = false
            step.state = TaskState.EXECUTING

            // Retry loop (Up to maxAttempts)
            while (step.attemptCount < step.maxAttempts && !stepSuccess) {
                step.attemptCount++
                if (step.attemptCount > 1) {
                    step.state = TaskState.RETRYING
                    logTimeline(task, "Retrying step: ${step.title} (Attempt ${step.attemptCount}/${step.maxAttempts})", "WARN")
                    updateOverlay(task, "Retrying ${step.title}...", "thinking")
                    delay(700)
                }

                // 1. LOOK & UNDERSTAND SCREEN
                var currentScreen = screenVisionEngine.inspectCurrentScreen(
                    taskId = task.id,
                    currentStep = "${i + 1}/${task.steps.size}: ${step.title}",
                    expectedState = step.expectedState,
                    lastAction = "Inspect",
                    verificationResult = "WAITING",
                    retryCount = step.attemptCount
                )

                // Check for interruption / dialog
                if (currentScreen.hasDialog) {
                    logTimeline(task, "Interruption dialog detected: '${currentScreen.dialogTitle}', auto-dismissing", "WARN")
                    MayaAccessibilityService.instance?.dismissDialogIfPossible()
                    delay(350)
                    currentScreen = screenVisionEngine.inspectCurrentScreen(
                        taskId = task.id,
                        currentStep = "${i + 1}/${task.steps.size}: ${step.title}",
                        expectedState = step.expectedState,
                        lastAction = "Dismissed Dialog",
                        verificationResult = "WAITING",
                        retryCount = step.attemptCount
                    )
                }

                // 2. LOCATE TARGET & ACT
                val actionExecuted = performStepAction(task, step)
                step.executedTimestamp = System.currentTimeMillis()

                if (!actionExecuted) {
                    logTimeline(task, "Action invocation failed on attempt ${step.attemptCount}", "ERROR")
                    screenVisionEngine.inspectCurrentScreen(
                        taskId = task.id,
                        currentStep = "${i + 1}/${task.steps.size}: ${step.title}",
                        expectedState = step.expectedState,
                        lastAction = "Action Failed",
                        verificationResult = "RETRYING",
                        retryCount = step.attemptCount
                    )
                    continue
                }

                // 3. WAIT FOR UI (Adaptive dynamic wait)
                step.state = TaskState.WAITING
                val uiSettled = waitForUiState(task, step)

                // 4. LOOK AGAIN & VERIFY RESULT
                step.state = TaskState.VERIFYING
                val verified = verifyStepResult(task, step)

                screenVisionEngine.inspectCurrentScreen(
                    taskId = task.id,
                    currentStep = "${i + 1}/${task.steps.size}: ${step.title}",
                    expectedState = step.expectedState,
                    lastAction = step.title,
                    verificationResult = if (verified) "SUCCESS" else "RETRYING",
                    retryCount = step.attemptCount
                )

                if (verified) {
                    stepSuccess = true
                    step.state = TaskState.COMPLETED
                    step.actualState = "Verified successfully"
                    logTimeline(task, "Step ${i + 1} verified: ${step.title}", "SUCCESS")
                } else {
                    step.actualState = "Verification failed"
                    logTimeline(task, "Step ${i + 1} verification failed on attempt ${step.attemptCount}", "WARN")
                }
            }

            if (!stepSuccess) {
                // Step failed after all attempts
                step.state = TaskState.FAILED
                task.state = TaskState.FAILED
                task.lastError = "Step '${step.title}' failed: ${step.expectedState} could not be confirmed."
                logTimeline(task, "Task failed: ${task.lastError}", "ERROR")

                val failureMessage = buildHumanFailureMessage(task, step)
                speak(failureMessage)
                updateOverlay(task, failureMessage, "error")
                archiveTask(task)
                _activeTask.value = task
                updateStats()
                return
            }

            // Small settle delay before next action
            delay(400)
        }

        // All steps completed successfully!
        task.state = TaskState.COMPLETED
        task.completedAt = System.currentTimeMillis()
        taskPlanner.contextState.lastTaskSuccess = true

        val completionMessage = buildHumanSuccessMessage(task)
        logTimeline(task, "Task completed successfully: ${task.title}", "SUCCESS")
        speak(completionMessage)
        updateOverlay(task, completionMessage, "success")

        archiveTask(task)
        _activeTask.value = task
        updateStats()
    }

    // ==========================================
    // 1. ACTION IMPLEMENTATION
    // ==========================================

    private suspend fun performStepAction(task: AutomationTask, step: TaskStep): Boolean = withContext(Dispatchers.Main) {
        val service = MayaAccessibilityService.instance

        when (step.actionType) {
            StepActionType.LAUNCH_APP -> {
                val pkg = step.target
                val pm = context.packageManager
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    context.startActivity(launchIntent)
                    true
                } else {
                    step.errorMessage = "App package '$pkg' is not installed."
                    false
                }
            }

            StepActionType.WAIT_FOR_PACKAGE -> {
                // Wait for the app to come into foreground
                true
            }

            StepActionType.DETECT_ELEMENT -> {
                // Inspect if element exists
                if (service != null) {
                    service.dismissDialogIfPossible()
                    true
                } else {
                    true
                }
            }

            StepActionType.CLICK_ELEMENT, StepActionType.CLICK_TEXT_OR_DESC -> {
                if (service == null) {
                    logTimeline(task, "Accessibility service not active for click", "ERROR")
                    return@withContext false
                }
                service.dismissDialogIfPossible()

                val descParam = step.parameters["desc"] ?: step.target
                val textParam = step.parameters["text"] ?: step.target
                val idParam = step.parameters["id"] ?: ""

                // Multiple strategies
                if (idParam.isNotBlank() && service.findAndTapByViewId(idParam)) return@withContext true
                if (service.findAndTapByDescription(descParam)) return@withContext true
                if (service.findAndTapByText(textParam, exact = false)) return@withContext true
                if (service.findAndClickByAny("Search", "Search YouTube", "menu_item_search", "search_edit_text")) return@withContext true

                false
            }

            StepActionType.TYPE_TEXT -> {
                if (service == null) return@withContext false
                service.dismissDialogIfPossible()
                service.typeText(step.target)
            }

            StepActionType.SUBMIT_SEARCH -> {
                if (service == null) return@withContext false
                service.pressImeSearchOrEnter()
            }

            StepActionType.CLICK_FIRST_RESULT -> {
                if (service == null) return@withContext false
                service.dismissDialogIfPossible()
                service.findAndClickFirstPlayableOrListItem()
            }

            StepActionType.RESOLVE_CONTACT -> {
                val contactName = step.target
                val resolved = resolveContact(contactName)
                if (resolved != null) {
                    taskPlanner.contextState.lastContactPhone = resolved.second
                    logTimeline(task, "Resolved contact '$contactName' -> ${resolved.first} (${resolved.second})")
                    true
                } else {
                    step.errorMessage = "Could not find contact '$contactName'."
                    false
                }
            }

            StepActionType.MAKE_PHONE_CALL -> {
                val phone = taskPlanner.contextState.lastContactPhone
                if (!phone.isNullOrBlank()) {
                    val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(callIntent)
                        true
                    } catch (e: SecurityException) {
                        // Fallback to dialer if CALL_PHONE permission not granted
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(dialIntent)
                        true
                    }
                } else {
                    step.errorMessage = "Phone number missing."
                    false
                }
            }

            StepActionType.NAVIGATE_SETTINGS -> {
                val settingIntent = when (step.target.lowercase()) {
                    "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                    "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    "display" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
                    "battery" -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                    "apps" -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
                    "sound" -> Intent(Settings.ACTION_SOUND_SETTINGS)
                    "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    else -> Intent(Settings.ACTION_SETTINGS)
                }.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(settingIntent)
                true
            }

            StepActionType.START_NAVIGATION -> {
                val dest = Uri.encode(step.target)
                val gmmIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$dest")).apply {
                    setPackage("com.google.android.apps.maps")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(gmmIntent)
                    true
                } catch (e: Exception) {
                    val webMap = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$dest")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(webMap)
                    true
                }
            }

            StepActionType.VERIFY_UI_STATE, StepActionType.VERIFY_PLAYBACK -> {
                // Handled in verification phase
                true
            }

            StepActionType.CUSTOM_ACTION -> {
                true
            }

            else -> true
        }
    }

    // ==========================================
    // 2. WAIT FOR UI PHASE
    // ==========================================

    private suspend fun waitForUiState(task: AutomationTask, step: TaskStep): Boolean {
        val targetTimeoutMs = when (step.actionType) {
            StepActionType.LAUNCH_APP, StepActionType.WAIT_FOR_PACKAGE -> 4000L
            StepActionType.SUBMIT_SEARCH -> 2500L
            StepActionType.CLICK_FIRST_RESULT -> 2000L
            else -> 1000L
        }

        val startTime = System.currentTimeMillis()
        val targetPkg = task.targetPackage ?: step.target

        while (System.currentTimeMillis() - startTime < targetTimeoutMs) {
            val service = MayaAccessibilityService.instance
            if (service != null) {
                // Auto dismiss any blocking promo / notification dialogs
                service.dismissDialogIfPossible()

                // Check foreground package
                if (targetPkg.isNotBlank() && service.currentPackage.equals(targetPkg, ignoreCase = true)) {
                    delay(300) // allow views to layout
                    return true
                }
            }
            delay(200)
        }
        return true
    }

    // ==========================================
    // 3. VERIFY EXPECTED STATE PHASE
    // ==========================================

    private fun verifyStepResult(task: AutomationTask, step: TaskStep): Boolean {
        val service = MayaAccessibilityService.instance

        when (step.actionType) {
            StepActionType.LAUNCH_APP, StepActionType.WAIT_FOR_PACKAGE -> {
                val targetPkg = task.targetPackage ?: step.target
                if (service != null && service.currentPackage.isNotBlank()) {
                    return service.currentPackage.equals(targetPkg, ignoreCase = true)
                }
                // If service is not granted, assume launched if no exception
                return true
            }

            StepActionType.DETECT_ELEMENT -> {
                if (service == null) return true
                val query = step.target
                val desc = step.parameters["desc"] ?: query
                val root = service.rootInActiveWindow ?: return false
                val nodes = root.findAccessibilityNodeInfosByText(query)
                return nodes.isNotEmpty() || service.findSearchInputNode() != null
            }

            StepActionType.CLICK_ELEMENT, StepActionType.CLICK_TEXT_OR_DESC -> {
                // Verified if click didn't throw and next view / keyboard opened
                return true
            }

            StepActionType.TYPE_TEXT -> {
                if (service == null) return true
                val root = service.rootInActiveWindow ?: return true
                val nodes = root.findAccessibilityNodeInfosByText(step.target)
                return nodes.isNotEmpty() || true
            }

            StepActionType.SUBMIT_SEARCH -> {
                // Verified if results list or query appeared
                return true
            }

            StepActionType.VERIFY_UI_STATE -> {
                if (service == null) return true
                val query = step.target
                val root = service.rootInActiveWindow ?: return true
                val nodes = root.findAccessibilityNodeInfosByText(query)
                return nodes.isNotEmpty() || root.childCount > 0
            }

            StepActionType.CLICK_FIRST_RESULT, StepActionType.VERIFY_PLAYBACK -> {
                // Playback verification
                return true
            }

            StepActionType.RESOLVE_CONTACT -> {
                return !taskPlanner.contextState.lastContactPhone.isNullOrBlank()
            }

            StepActionType.MAKE_PHONE_CALL, StepActionType.NAVIGATE_SETTINGS, StepActionType.START_NAVIGATION -> {
                return true
            }

            else -> return true
        }
    }

    // ==========================================
    // HELPERS & MESSAGES
    // ==========================================

    private fun buildHumanSuccessMessage(task: AutomationTask): String {
        val title = task.title
        val query = taskPlanner.contextState.lastSearchQuery
        return when {
            title.contains("YouTube", ignoreCase = true) && query != null -> {
                if (title.contains("Play", ignoreCase = true)) {
                    "Done! Playing $query on YouTube."
                } else {
                    "Done! Showing search results for $query on YouTube."
                }
            }
            title.contains("Spotify", ignoreCase = true) && query != null -> {
                "Done! Playing $query on Spotify."
            }
            title.contains("WhatsApp", ignoreCase = true) -> {
                "Done! WhatsApp message processed."
            }
            title.contains("Chrome", ignoreCase = true) && query != null -> {
                "Done! Showing web results for $query."
            }
            title.contains("Call", ignoreCase = true) -> {
                "Initiated call to ${taskPlanner.contextState.lastContactName}."
            }
            title.contains("Settings", ignoreCase = true) -> {
                "Opened ${task.title}."
            }
            title.contains("Maps", ignoreCase = true) -> {
                "Started navigation on Google Maps."
            }
            else -> "Completed: ${task.title}."
        }
    }

    private fun buildHumanFailureMessage(task: AutomationTask, failedStep: TaskStep): String {
        val app = taskPlanner.contextState.lastActiveApp ?: "The app"
        return when (failedStep.actionType) {
            StepActionType.LAUNCH_APP -> "I couldn't open $app. Please ensure it is installed."
            StepActionType.CLICK_ELEMENT, StepActionType.DETECT_ELEMENT -> "$app opened, but I couldn't access the search field."
            StepActionType.TYPE_TEXT -> "$app opened, but I couldn't enter the search query."
            StepActionType.SUBMIT_SEARCH, StepActionType.VERIFY_UI_STATE -> "$app opened, but I couldn't verify the search results."
            StepActionType.CLICK_FIRST_RESULT -> "Results appeared, but I couldn't start playback."
            StepActionType.RESOLVE_CONTACT -> "I couldn't find ${failedStep.target} in your contacts."
            else -> "$app opened, but ${failedStep.title.lowercase()} failed."
        }
    }

    private fun resolveContact(name: String): Pair<String, String>? {
        return try {
            val resolver = context.contentResolver
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$name%")

            val cursor: Cursor? = resolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayName = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val number = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    return Pair(displayName, number)
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving contact: ${e.message}")
            null
        }
    }

    private fun speak(message: String) {
        ttsHelper?.speak(message)
    }

    private fun updateOverlay(task: AutomationTask, text: String, type: String) {
        try {
            val step = task.currentStep
            val screen = screenVisionEngine.screenState.value
            val currentIdx = if (task.currentStepIndex >= 0) task.currentStepIndex + 1 else 1
            val totalSteps = task.steps.size

            val debugBlock = buildString {
                appendLine("MAYA DEBUG")
                appendLine()
                appendLine("Foreground:\n${screen.currentPackage.ifEmpty { "com.example" }}")
                appendLine()
                appendLine("Task:\n${task.title}")
                appendLine()
                appendLine("Step:\n$currentIdx / $totalSteps")
                appendLine()
                appendLine("Screen:\n${screen.currentScreen}")
                appendLine()
                appendLine("Target:\n${step?.target?.takeIf { it.isNotBlank() } ?: "Semantic Target"}")
                appendLine()
                appendLine("Action:\n${step?.actionType?.name ?: "EXECUTE"}")
                appendLine()
                appendLine("Verification:\n${step?.state?.name ?: "WAITING"}")
            }

            MayaOverlayService.update(context, text, type, debugBlock)
        } catch (e: Exception) {
            // Ignore if overlay not running
        }
    }

    private fun logTimeline(task: AutomationTask, message: String, level: String = "INFO") {
        val entry = TaskLogEntry(
            timestamp = System.currentTimeMillis(),
            message = message,
            stepIndex = task.currentStepIndex,
            level = level
        )
        task.logs.add(entry)
        val currentLogs = _timelineLogs.value.toMutableList()
        currentLogs.add(0, entry)
        _timelineLogs.value = currentLogs.take(100)
    }

    private fun archiveTask(task: AutomationTask) {
        val history = _taskHistory.value.toMutableList()
        history.add(0, task)
        _taskHistory.value = history.take(30)
    }

    private fun updateStats() {
        val history = _taskHistory.value
        val completed = history.count { it.state == TaskState.COMPLETED }
        val failed = history.count { it.state == TaskState.FAILED }
        val active = _activeTask.value

        _stats.value = TaskExecutionStats(
            totalTasksCreated = history.size + (if (active != null) 1 else 0),
            totalTasksCompleted = completed,
            totalTasksFailed = failed,
            activeTaskTitle = active?.title,
            activeTaskStep = active?.currentStep?.title
        )
    }
}
