package com.example.assistant.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Lifecycle states of an AI Task executed by MAYA.
 */
enum class TaskState(val label: String) {
    RECEIVED("Received"),
    PLANNING("Planning"),
    EXECUTING("Executing"),
    WAITING("Waiting for UI"),
    VERIFYING("Verifying Result"),
    RETRYING("Retrying"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled")
}

/**
 * Specific executable action types that UI steps can perform.
 */
enum class StepActionType {
    LAUNCH_APP,
    WAIT_FOR_PACKAGE,
    DETECT_ELEMENT,
    CLICK_ELEMENT,
    CLICK_TEXT_OR_DESC,
    TYPE_TEXT,
    SUBMIT_SEARCH,
    VERIFY_UI_STATE,
    CLICK_FIRST_RESULT,
    VERIFY_PLAYBACK,
    SWIPE,
    SCROLL,
    RESOLVE_CONTACT,
    MAKE_PHONE_CALL,
    SEND_SMS_MESSAGE,
    SEND_WHATSAPP_MESSAGE,
    NAVIGATE_SETTINGS,
    START_NAVIGATION,
    SYSTEM_TOGGLE,
    CUSTOM_ACTION
}

/**
 * An individual atomic step within a multi-step task plan.
 */
data class TaskStep(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val actionType: StepActionType,
    val target: String = "",
    val parameters: Map<String, String> = emptyMap(),
    val expectedState: String = "",
    var actualState: String = "",
    var state: TaskState = TaskState.RECEIVED,
    var attemptCount: Int = 0,
    val maxAttempts: Int = 3,
    var errorMessage: String? = null,
    var executedTimestamp: Long? = null
) {
    fun isFinished(): Boolean = state == TaskState.COMPLETED || state == TaskState.FAILED || state == TaskState.CANCELLED
}

/**
 * Log entry for the real-time developer timeline.
 */
data class TaskLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp)),
    val message: String,
    val stepIndex: Int = 0,
    val level: String = "INFO"
)

/**
 * A stateful automation task containing one or more atomic steps.
 */
data class AutomationTask(
    val id: String = UUID.randomUUID().toString().take(8),
    val originalCommand: String,
    val title: String,
    val steps: List<TaskStep>,
    var currentStepIndex: Int = 0,
    var state: TaskState = TaskState.RECEIVED,
    val createdAt: Long = System.currentTimeMillis(),
    var completedAt: Long? = null,
    val targetPackage: String? = null,
    val logs: MutableList<TaskLogEntry> = mutableListOf(),
    var lastSpokenUpdate: String = "",
    var isPaused: Boolean = false,
    var lastError: String? = null
) {
    val currentStep: TaskStep?
        get() = steps.getOrNull(currentStepIndex)

    val progressFraction: Float
        get() = if (steps.isEmpty()) 0f else currentStepIndex.toFloat() / steps.size.toFloat()

    val stepSummary: String
        get() = if (steps.isEmpty()) "0 / 0" else "${(currentStepIndex + 1).coerceAtMost(steps.size)} / ${steps.size}"

    fun addLog(message: String, level: String = "INFO") {
        logs.add(TaskLogEntry(
            timestamp = System.currentTimeMillis(),
            message = message,
            stepIndex = currentStepIndex,
            level = level
        ))
    }
}

/**
 * Retained context for follow-up conversational commands.
 */
data class TaskContextState(
    var lastActiveApp: String? = null,
    var lastPackage: String? = null,
    var lastSearchQuery: String? = null,
    var lastContactName: String? = null,
    var lastContactPhone: String? = null,
    var lastMediaItem: String? = null,
    var lastCommand: String? = null,
    var lastTaskSuccess: Boolean = false
)

/**
 * High-level task execution statistics for telemetry and debug.
 */
data class TaskExecutionStats(
    val totalTasksCreated: Int = 0,
    val totalTasksCompleted: Int = 0,
    val totalTasksFailed: Int = 0,
    val activeTaskTitle: String? = null,
    val activeTaskStep: String? = null
)
