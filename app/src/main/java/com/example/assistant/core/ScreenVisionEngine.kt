package com.example.assistant.core

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.example.assistant.services.MayaAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Semantic roles for visual screen elements detected by MAYA Vision.
 */
enum class VisualRole {
    SEARCH_INPUT,
    SEARCH_BUTTON,
    PLAY_BUTTON,
    FIRST_RESULT,
    LIST_ITEM,
    CONTACT_ITEM,
    SEND_BUTTON,
    URL_BAR,
    BACK_BUTTON,
    CLOSE_BUTTON,
    DIALOG_DISMISS,
    DIALOG_CONFIRM,
    GENERIC_BUTTON,
    TEXT_LABEL,
    UNKNOWN
}

/**
 * A semantically parsed interactive or visible target on screen.
 */
data class VisualTarget(
    val id: String,
    val text: String,
    val contentDescription: String,
    val viewId: String,
    val className: String,
    val bounds: Rect,
    val role: VisualRole,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean
) {
    val centerX: Float
        get() = bounds.centerX().toFloat()

    val centerY: Float
        get() = bounds.centerY().toFloat()

    val label: String
        get() = when {
            text.isNotBlank() -> text
            contentDescription.isNotBlank() -> contentDescription
            viewId.isNotBlank() -> viewId.substringAfterLast(":id/")
            else -> role.name
        }
}

/**
 * Real-time Screen State Model maintained during task execution and UI monitoring.
 */
data class ScreenStateModel(
    val currentPackage: String = "",
    val currentActivity: String = "",
    val currentScreen: String = "Unknown",
    val visibleElements: List<VisualTarget> = emptyList(),
    val taskId: String? = null,
    val currentStep: String? = null,
    val expectedState: String = "",
    val lastAction: String = "",
    val verificationResult: String = "WAITING", // WAITING, SUCCESS, RETRYING, FAILED
    val retryCount: Int = 0,
    val hasDialog: Boolean = false,
    val dialogTitle: String? = null,
    val isKeyboardOpen: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    val elementCount: Int
        get() = visibleElements.size

    val summary: String
        get() = "$currentScreen ($currentPackage) - $elementCount elements"
}

/**
 * MAYA Screen Vision & Visual Understanding Engine.
 * Implements semantic reasoning over accessibility trees and visual layout:
 * LOOK -> UNDERSTAND SCREEN -> LOCATE TARGET -> ACT -> LOOK AGAIN -> VERIFY.
 */
class ScreenVisionEngine private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ScreenVisionEngine"

        @Volatile
        private var instance: ScreenVisionEngine? = null

        fun getInstance(context: Context): ScreenVisionEngine {
            return instance ?: synchronized(this) {
                instance ?: ScreenVisionEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    private val _screenState = MutableStateFlow(ScreenStateModel())
    val screenState: StateFlow<ScreenStateModel> = _screenState.asStateFlow()

    /**
     * Inspects the current screen through active accessibility service,
     * extracts all visible/interactive elements, determines semantic screen name,
     * and returns an updated ScreenStateModel.
     */
    fun inspectCurrentScreen(
        taskId: String? = null,
        currentStep: String? = null,
        expectedState: String = "",
        lastAction: String = "",
        verificationResult: String = "WAITING",
        retryCount: Int = 0
    ): ScreenStateModel {
        val service = MayaAccessibilityService.instance
        if (service == null) {
            val emptyModel = ScreenStateModel(
                currentPackage = "Accessibility service inactive",
                currentActivity = "",
                currentScreen = "Service Unavailable",
                taskId = taskId,
                currentStep = currentStep,
                expectedState = expectedState,
                lastAction = lastAction,
                verificationResult = verificationResult,
                retryCount = retryCount
            )
            _screenState.value = emptyModel
            return emptyModel
        }

        val rootNode = service.rootInActiveWindow
        val rawPackage = service.currentPackage.ifBlank { rootNode?.packageName?.toString() ?: "" }
        val rawActivity = service.currentActivity

        val elements = mutableListOf<VisualTarget>()
        var dialogDetected = false
        var dialogText: String? = null
        var keyboardOpen = false

        if (rootNode != null) {
            parseNodeHierarchy(rootNode, elements, 0)
            // Check for dialogs or popups
            val dialogInfo = inspectForDialogs(rootNode)
            if (dialogInfo != null) {
                dialogDetected = true
                dialogText = dialogInfo
            }
            // Check for keyboard presence (input focus or IME view)
            keyboardOpen = elements.any { it.isEditable && it.bounds.bottom > 1200 }
        }

        val semanticScreenName = determineScreenName(rawPackage, rawActivity, elements, dialogDetected)

        val model = ScreenStateModel(
            currentPackage = rawPackage,
            currentActivity = rawActivity,
            currentScreen = semanticScreenName,
            visibleElements = elements,
            taskId = taskId,
            currentStep = currentStep,
            expectedState = expectedState,
            lastAction = lastAction,
            verificationResult = verificationResult,
            retryCount = retryCount,
            hasDialog = dialogDetected,
            dialogTitle = dialogText,
            isKeyboardOpen = keyboardOpen,
            timestamp = System.currentTimeMillis()
        )

        _screenState.value = model
        return model
    }

    private fun parseNodeHierarchy(
        node: AccessibilityNodeInfo,
        elements: MutableList<VisualTarget>,
        depth: Int
    ) {
        if (depth > 25) return // Prevent stack overflow

        val rect = Rect()
        node.getBoundsInScreen(rect)

        val text = node.text?.toString()?.trim() ?: ""
        val desc = node.contentDescription?.toString()?.trim() ?: ""
        val viewId = node.viewIdResourceName?.toString() ?: ""
        val className = node.className?.toString() ?: ""

        val isVisible = rect.width() > 0 && rect.height() > 0
        val isClickable = node.isClickable
        val isEditable = node.isEditable || className.contains("EditText", ignoreCase = true)
        val isScrollable = node.isScrollable

        if (isVisible && (text.isNotBlank() || desc.isNotBlank() || viewId.isNotBlank() || isClickable || isEditable)) {
            val role = classifyRole(node, text, desc, viewId, className, rect)
            elements.add(
                VisualTarget(
                    id = "el_${elements.size}_${viewId.substringAfterLast(":id/")}",
                    text = text,
                    contentDescription = desc,
                    viewId = viewId,
                    className = className,
                    bounds = rect,
                    role = role,
                    isClickable = isClickable,
                    isEditable = isEditable,
                    isScrollable = isScrollable
                )
            )
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            parseNodeHierarchy(child, elements, depth + 1)
        }
    }

    private fun classifyRole(
        node: AccessibilityNodeInfo,
        text: String,
        desc: String,
        viewId: String,
        className: String,
        rect: Rect
    ): VisualRole {
        val lowerText = text.lowercase(Locale.getDefault())
        val lowerDesc = desc.lowercase(Locale.getDefault())
        val lowerId = viewId.lowercase(Locale.getDefault())
        val lowerClass = className.lowercase(Locale.getDefault())

        // Search inputs
        if (node.isEditable || lowerClass.contains("edittext") || lowerId.contains("search_src_text") || lowerId.contains("search_edit_text")) {
            return VisualRole.SEARCH_INPUT
        }

        // URL / Address bar
        if (lowerId.contains("url_bar") || lowerDesc.contains("search or type url")) {
            return VisualRole.URL_BAR
        }

        // Search buttons/icons
        if (lowerDesc.contains("search") || lowerText.contains("search") || lowerId.contains("search") || lowerDesc.contains("khojo")) {
            return VisualRole.SEARCH_BUTTON
        }

        // Send buttons (WhatsApp, SMS, etc.)
        if (lowerDesc.contains("send") || lowerId.contains("send") || lowerText == "send") {
            return VisualRole.SEND_BUTTON
        }

        // Play buttons
        if (lowerDesc.contains("play") || lowerId.contains("play") || lowerDesc.contains("chalao")) {
            return VisualRole.PLAY_BUTTON
        }

        // Dialog dismiss buttons
        if (lowerText in listOf("not now", "dismiss", "skip", "cancel", "no thanks", "later", "close", "got it")) {
            return VisualRole.DIALOG_DISMISS
        }

        // Dialog confirm buttons
        if (lowerText in listOf("allow", "ok", "yes", "accept", "continue", "proceed")) {
            return VisualRole.DIALOG_CONFIRM
        }

        // First playable or prominent list card
        if (rect.width() > 150 && rect.height() > 80 && rect.top > 200 && (node.isClickable || lowerClass.contains("card") || lowerClass.contains("layout"))) {
            return VisualRole.FIRST_RESULT
        }

        if (node.isClickable) {
            return VisualRole.GENERIC_BUTTON
        }

        if (text.isNotBlank()) {
            return VisualRole.TEXT_LABEL
        }

        return VisualRole.UNKNOWN
    }

    private fun determineScreenName(
        pkg: String,
        activity: String,
        elements: List<VisualTarget>,
        hasDialog: Boolean
    ): String {
        if (hasDialog) return "Interruption / Dialog Screen"

        val lowerPkg = pkg.lowercase(Locale.getDefault())
        val hasSearchInput = elements.any { it.role == VisualRole.SEARCH_INPUT }
        val hasPlay = elements.any { it.role == VisualRole.PLAY_BUTTON }

        return when {
            lowerPkg.contains("youtube") -> {
                when {
                    elements.any { it.viewId.contains("player") || it.className.contains("Player") } -> "YouTube Video Player"
                    hasSearchInput -> "YouTube Search"
                    elements.any { it.role == VisualRole.FIRST_RESULT } -> "YouTube Search Results"
                    else -> "YouTube Home"
                }
            }
            lowerPkg.contains("whatsapp") -> {
                when {
                    elements.any { it.role == VisualRole.SEND_BUTTON || it.viewId.contains("entry") } -> "WhatsApp Conversation"
                    hasSearchInput -> "WhatsApp Search Contact"
                    else -> "WhatsApp Chats"
                }
            }
            lowerPkg.contains("chrome") -> {
                when {
                    elements.any { it.role == VisualRole.URL_BAR || hasSearchInput } -> "Chrome Address Input"
                    elements.any { it.text.contains("Google", ignoreCase = true) } -> "Google Search Results"
                    else -> "Chrome Web Page"
                }
            }
            lowerPkg.contains("spotify") -> {
                when {
                    hasPlay -> "Spotify Player"
                    hasSearchInput -> "Spotify Search"
                    else -> "Spotify Home"
                }
            }
            lowerPkg.contains("settings") -> {
                val sub = activity.substringAfterLast('.').replace("Settings", "").replace("Activity", "")
                if (sub.isNotBlank()) "Settings: $sub" else "Settings Main"
            }
            lowerPkg.contains("maps") -> "Google Maps Navigation"
            lowerPkg.contains("instagram") -> {
                when {
                    hasSearchInput -> "Instagram Search"
                    elements.any { it.text.contains("profile", ignoreCase = true) } -> "Instagram Profile"
                    else -> "Instagram Feed"
                }
            }
            else -> {
                val cleanApp = pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                "$cleanApp Active Screen"
            }
        }
    }

    private fun inspectForDialogs(node: AccessibilityNodeInfo): String? {
        val cls = node.className?.toString() ?: ""
        if (cls.contains("Dialog", ignoreCase = true) || cls.contains("AlertDialog", ignoreCase = true)) {
            return node.text?.toString() ?: "System Dialog"
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = inspectForDialogs(child)
            if (found != null) return found
        }
        return null
    }

    /**
     * Semantically locates an element by target role or query text/description.
     */
    fun findTarget(role: VisualRole, query: String = ""): VisualTarget? {
        val currentElements = _screenState.value.visibleElements
        val lowerQuery = query.lowercase(Locale.getDefault())

        // 1. Direct role match with matching query if query is provided
        if (lowerQuery.isNotBlank()) {
            val matchedByText = currentElements.firstOrNull {
                it.role == role && (it.text.lowercase().contains(lowerQuery) || it.contentDescription.lowercase().contains(lowerQuery))
            }
            if (matchedByText != null) return matchedByText

            // Any element matching text regardless of role
            val generalTextMatch = currentElements.firstOrNull {
                it.text.lowercase().contains(lowerQuery) || it.contentDescription.lowercase().contains(lowerQuery)
            }
            if (generalTextMatch != null) return generalTextMatch
        }

        // 2. Pure role match
        return currentElements.firstOrNull { it.role == role }
    }
}
