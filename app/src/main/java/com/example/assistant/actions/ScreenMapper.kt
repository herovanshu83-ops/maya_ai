package com.example.assistant.actions

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.example.assistant.services.MayaAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScreenMapper(
    private val context: Context
) {

    data class ScreenElement(
        val id: String,
        val text: String,
        val contentDescription: String,
        val className: String,
        val packageName: String,
        val bounds: Rect,
        val isClickable: Boolean,
        val isFocusable: Boolean,
        val isScrollable: Boolean,
        val isEditable: Boolean,
        val isChecked: Boolean,
        val isSelected: Boolean,
        val depth: Int,
        val children: List<ScreenElement> = emptyList()
    )

    data class ScreenMap(
        val timestamp: Long,
        val packageName: String,
        val activityName: String,
        val elements: List<ScreenElement>,
        val visibleElements: List<ScreenElement>,
        val clickableElements: List<ScreenElement>,
        val editableElements: List<ScreenElement>,
        val scrollableElements: List<ScreenElement>
    )

    private var currentScreenMap: ScreenMap? = null
    private val _screenMapFlow = MutableStateFlow<ScreenMap?>(null)
    val screenMapFlow: StateFlow<ScreenMap?> = _screenMapFlow.asStateFlow()

    fun mapCurrentScreen(): ScreenMap {
        val service = MayaAccessibilityService.instance
        val nodeInfo = service?.rootInActiveWindow ?: return emptyScreenMap()

        val elements = mutableListOf<ScreenElement>()
        traverseNode(nodeInfo, elements, 0)

        val screenMap = ScreenMap(
            timestamp = System.currentTimeMillis(),
            packageName = nodeInfo.packageName?.toString() ?: "unknown",
            activityName = getActivityName(nodeInfo),
            elements = elements,
            visibleElements = elements.filter { it.bounds.width() > 0 && it.bounds.height() > 0 },
            clickableElements = elements.filter { it.isClickable },
            editableElements = elements.filter { it.isEditable || it.className.contains("EditText", ignoreCase = true) },
            scrollableElements = elements.filter { it.isScrollable }
        )

        currentScreenMap = screenMap
        _screenMapFlow.value = screenMap

        return screenMap
    }

    @Suppress("DEPRECATION")
    private fun traverseNode(node: AccessibilityNodeInfo, elements: MutableList<ScreenElement>, depth: Int) {
        val compatNode = AccessibilityNodeInfoCompat.wrap(node)
        val rect = Rect()
        compatNode.getBoundsInScreen(rect)

        val element = ScreenElement(
            id = compatNode.viewIdResourceName ?: "id_${System.identityHashCode(node)}",
            text = compatNode.text?.toString() ?: "",
            contentDescription = compatNode.contentDescription?.toString() ?: "",
            className = compatNode.className?.toString() ?: "",
            packageName = compatNode.packageName?.toString() ?: "",
            bounds = rect,
            isClickable = compatNode.isClickable,
            isFocusable = compatNode.isFocusable,
            isScrollable = compatNode.isScrollable,
            isEditable = compatNode.isEditable,
            isChecked = compatNode.isChecked,
            isSelected = compatNode.isSelected,
            depth = depth,
            children = emptyList()
        )

        elements.add(element)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNode(child, elements, depth + 1)
            }
        }
    }

    private fun getActivityName(node: AccessibilityNodeInfo): String {
        return node.packageName?.toString() ?: "Unknown"
    }

    private fun emptyScreenMap(): ScreenMap {
        return ScreenMap(
            timestamp = System.currentTimeMillis(),
            packageName = "unknown",
            activityName = "unknown",
            elements = emptyList(),
            visibleElements = emptyList(),
            clickableElements = emptyList(),
            editableElements = emptyList(),
            scrollableElements = emptyList()
        )
    }

    fun findElementByText(text: String): ScreenElement? {
        val map = currentScreenMap ?: mapCurrentScreen()
        return map.elements.find {
            it.text.contains(text, ignoreCase = true) ||
            it.contentDescription.contains(text, ignoreCase = true)
        }
    }

    fun findElementById(id: String): ScreenElement? {
        val map = currentScreenMap ?: mapCurrentScreen()
        return map.elements.find { it.id.contains(id, ignoreCase = true) }
    }

    fun findElementsByClass(className: String): List<ScreenElement> {
        val map = currentScreenMap ?: mapCurrentScreen()
        return map.elements.filter { it.className.contains(className, ignoreCase = true) }
    }

    fun findClickableElements(): List<ScreenElement> {
        val map = currentScreenMap ?: mapCurrentScreen()
        return map.clickableElements
    }

    fun findEditableElements(): List<ScreenElement> {
        val map = currentScreenMap ?: mapCurrentScreen()
        return map.editableElements
    }
}
