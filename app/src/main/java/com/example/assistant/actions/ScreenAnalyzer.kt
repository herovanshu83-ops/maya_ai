package com.example.assistant.actions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.example.assistant.services.MayaAccessibilityService

/**
 * Identifies and classifies screen elements (text, buttons, inputs, images).
 * Computes summaries, element counts, and interactive target bounds.
 */
class ScreenAnalyzer(
    private val context: Context
) {
    companion object {
        private const val TAG = "ScreenAnalyzer"
    }

    data class ScreenElement(
        val text: String,
        val type: String,
        val bounds: Rect,
        val isClickable: Boolean,
        val isEditable: Boolean,
        val isVisible: Boolean,
        val confidence: Float
    )

    data class ScreenAnalysis(
        val elements: List<ScreenElement>,
        val summary: String,
        val hasText: Boolean,
        val hasImages: Boolean,
        val hasButtons: Boolean,
        val hasInputs: Boolean,
        val clickableCount: Int,
        val textCount: Int
    )

    fun analyzeScreen(bitmap: Bitmap? = null): ScreenAnalysis {
        val elements = mutableListOf<ScreenElement>()

        try {
            // Traverse active accessibility hierarchy
            val service = MayaAccessibilityService.instance
            val rootNode = service?.rootInActiveWindow

            if (rootNode != null) {
                val mapper = ScreenMapper(context)
                val screenMap = mapper.mapCurrentScreen()

                screenMap.elements.forEach { elem ->
                    val bounds = elem.bounds
                    val isVisible = bounds.width() > 0 && bounds.height() > 0
                    val displayText = when {
                        elem.text.isNotBlank() -> elem.text.trim()
                        elem.contentDescription.isNotBlank() -> elem.contentDescription.trim()
                        else -> ""
                    }

                    val className = elem.className.lowercase()
                    val type = when {
                        elem.isEditable || className.contains("edittext") || className.contains("input") -> "input"
                        elem.isClickable || className.contains("button") -> "button"
                        className.contains("image") || className.contains("icon") -> "image"
                        displayText.isNotBlank() -> "text"
                        else -> "element"
                    }

                    if (isVisible && (displayText.isNotBlank() || type == "button" || type == "input" || type == "image")) {
                        elements.add(
                            ScreenElement(
                                text = displayText,
                                type = type,
                                bounds = bounds,
                                isClickable = elem.isClickable,
                                isEditable = elem.isEditable,
                                isVisible = isVisible,
                                confidence = 0.95f
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Accessibility analysis error: ${e.message}", e)
        }

        // If elements are empty (e.g., accessibility node not available), generate fallback representation
        if (elements.isEmpty()) {
            if (bitmap != null) {
                elements.add(
                    ScreenElement(
                        text = "Current Screen Canvas (${bitmap.width}x${bitmap.height})",
                        type = "image",
                        bounds = Rect(0, 0, bitmap.width, bitmap.height),
                        isClickable = false,
                        isEditable = false,
                        isVisible = true,
                        confidence = 0.8f
                    )
                )
            } else {
                elements.add(
                    ScreenElement(
                        text = "MAYA Interface",
                        type = "text",
                        bounds = Rect(0, 0, 1080, 1920),
                        isClickable = true,
                        isEditable = false,
                        isVisible = true,
                        confidence = 0.9f
                    )
                )
            }
        }

        val textElements = elements.filter { it.type == "text" }
        val buttonElements = elements.filter { it.type == "button" }
        val inputElements = elements.filter { it.type == "input" }
        val imageElements = elements.filter { it.type == "image" }

        val hasText = textElements.isNotEmpty()
        val hasButtons = buttonElements.isNotEmpty()
        val hasInputs = inputElements.isNotEmpty()
        val hasImages = imageElements.isNotEmpty()
        val clickableCount = elements.count { it.isClickable }
        val textCount = textElements.size

        val summary = generateSummary(elements)

        return ScreenAnalysis(
            elements = elements,
            summary = summary,
            hasText = hasText,
            hasImages = hasImages,
            hasButtons = hasButtons,
            hasInputs = hasInputs,
            clickableCount = clickableCount,
            textCount = textCount
        )
    }

    private fun generateSummary(elements: List<ScreenElement>): String {
        val textElements = elements.filter { it.type == "text" && it.text.isNotBlank() }
        val buttonElements = elements.filter { it.type == "button" }
        val inputElements = elements.filter { it.type == "input" }
        val imageElements = elements.filter { it.type == "image" }

        val parts = mutableListOf<String>()

        if (textElements.isNotEmpty()) {
            val texts = textElements.take(4).map { "\"${it.text}\"" }.joinToString(", ")
            parts.add("I see text: $texts")
        } else {
            parts.add("No textual labels visible")
        }

        if (buttonElements.isNotEmpty()) {
            val namedButtons = buttonElements.filter { it.text.isNotBlank() }.take(3).map { it.text }
            if (namedButtons.isNotEmpty()) {
                parts.add("${buttonElements.size} buttons visible including ${namedButtons.joinToString(", ")}")
            } else {
                parts.add("${buttonElements.size} buttons visible")
            }
        }

        if (inputElements.isNotEmpty()) {
            parts.add("${inputElements.size} input fields available")
        }

        if (imageElements.isNotEmpty()) {
            parts.add("${imageElements.size} image elements visible")
        }

        return parts.joinToString(". ") + "."
    }
}
