package com.example.assistant.actions

import android.content.Context
import android.util.Log
import com.example.assistant.audio.TextToSpeechHelper

/**
 * Voice & Natural Language handler for Screen Reading and Visual AI.
 */
class ScreenCommandHandler(
    private val context: Context,
    private val screenReader: ScreenReader = ScreenReader.getInstance(context),
    private val screenHighlight: ScreenHighlightOverlay = ScreenHighlightOverlay.getInstance(context),
    private val screenCapture: ScreenCaptureService = ScreenCaptureService.getInstance(context),
    private val textToSpeech: TextToSpeechHelper = TextToSpeechHelper.getInstance(context)
) {
    companion object {
        private const val TAG = "ScreenCommandHandler"

        @Volatile
        private var instance: ScreenCommandHandler? = null

        fun getInstance(context: Context): ScreenCommandHandler {
            return instance ?: synchronized(this) {
                instance ?: ScreenCommandHandler(context.applicationContext).also { instance = it }
            }
        }
    }

    private val analyzer = ScreenAnalyzer(context)

    fun isScreenCommand(command: String): Boolean {
        val lower = command.lowercase().trim()
        return lower.contains("screen") ||
                lower.contains("what do you see") ||
                lower.contains("what can you see") ||
                lower.contains("describe what you see") ||
                lower.contains("highlight")
    }

    fun handleCommand(command: String): String {
        val lowerCommand = command.lowercase().trim()

        return when {
            // Read Screen
            lowerCommand.contains("screen") && (lowerCommand.contains("read") || lowerCommand.contains("speak")) -> {
                handleReadScreen()
            }

            // What's on screen / What is on screen
            lowerCommand.contains("what") && lowerCommand.contains("screen") -> {
                handleWhatOnScreen()
            }

            // Find on screen
            (lowerCommand.contains("find") || lowerCommand.contains("search") || lowerCommand.contains("where is")) && lowerCommand.contains("screen") -> {
                handleFindOnScreen(command)
            }

            // Find button
            lowerCommand.contains("find button") || lowerCommand.contains("where is the button") -> {
                handleFindSpecificType("button")
            }

            // Find image
            lowerCommand.contains("find image") || lowerCommand.contains("find icon") -> {
                handleFindSpecificType("image")
            }

            // Highlight screen / Show elements
            lowerCommand.contains("highlight") || lowerCommand.contains("show me screen elements") || lowerCommand.contains("show elements") -> {
                handleHighlightScreen()
            }

            // Hide highlights
            lowerCommand.contains("hide") && lowerCommand.contains("highlight") -> {
                screenHighlight.hideHighlights()
                val msg = "Highlights hidden."
                textToSpeech.speak(msg)
                msg
            }

            // Describe screen
            lowerCommand.contains("describe") -> {
                handleDescribeScreen()
            }

            // What can you see / Look at screen
            lowerCommand.contains("see") || lowerCommand.contains("look") -> {
                handleWhatCanYouSee()
            }

            else -> {
                val msg = "Screen command not recognized. Try: \"Read screen\", \"What's on screen\", \"Find Sign In on screen\", or \"Highlight screen\"."
                textToSpeech.speak(msg)
                msg
            }
        }
    }

    private fun handleReadScreen(): String {
        val readingStarted = screenReader.readScreenAsync()
        return if (readingStarted) {
            "Reading screen contents aloud..."
        } else {
            "Screen reading is already in progress."
        }
    }

    private fun handleWhatOnScreen(): String {
        return try {
            val bitmap = screenCapture.captureNow()
            val analysis = analyzer.analyzeScreen(bitmap)
            val summary = analysis.summary
            textToSpeech.speak(summary)
            summary
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleWhatOnScreen", e)
            val errorMsg = "Could not analyze the screen."
            textToSpeech.speak(errorMsg)
            errorMsg
        }
    }

    private fun handleFindOnScreen(command: String): String {
        val pattern = Regex("(?i)(?:find|where is|search for)\\s+(.+?)(?:\\s+on screen|$)")
        val match = pattern.find(command)
        val targetText = match?.groupValues?.get(1)?.trim() ?: ""

        if (targetText.isBlank()) {
            val prompt = "What text or element would you like me to find on the screen?"
            textToSpeech.speak(prompt)
            return prompt
        }

        val foundElement = screenReader.findElementOnScreen(targetText)
        return if (foundElement != null) {
            // Also highlight the found element
            screenHighlight.showHighlights(listOf(foundElement))
            val bounds = foundElement.bounds
            val label = if (foundElement.text.isNotBlank()) foundElement.text else foundElement.type
            val reply = "Found \"$label\" on screen at coordinates X: ${bounds.centerX()}, Y: ${bounds.centerY()}."
            textToSpeech.speak(reply)
            reply
        } else {
            val reply = "I could not locate \"$targetText\" on the visible screen."
            textToSpeech.speak(reply)
            reply
        }
    }

    private fun handleFindSpecificType(type: String): String {
        return try {
            val bitmap = screenCapture.captureNow()
            val analysis = analyzer.analyzeScreen(bitmap)
            val matched = analysis.elements.filter { it.type.equals(type, ignoreCase = true) }

            if (matched.isNotEmpty()) {
                screenHighlight.showHighlights(matched)
                val labels = matched.filter { it.text.isNotBlank() }.map { it.text }.take(3)
                val reply = if (labels.isNotEmpty()) {
                    "Found ${matched.size} $type elements: ${labels.joinToString(", ")}."
                } else {
                    "Found ${matched.size} $type elements on screen."
                }
                textToSpeech.speak(reply)
                reply
            } else {
                val reply = "No $type elements found on the screen."
                textToSpeech.speak(reply)
                reply
            }
        } catch (e: Exception) {
            "Could not search for $type elements."
        }
    }

    private fun handleHighlightScreen(): String {
        return try {
            val bitmap = screenCapture.captureNow()
            val analysis = analyzer.analyzeScreen(bitmap)

            if (analysis.elements.isEmpty()) {
                val msg = "No interactive elements detected on the active screen."
                textToSpeech.speak(msg)
                return msg
            }

            screenHighlight.showHighlights(analysis.elements)
            val msg = "Highlighting ${analysis.elements.size} detected screen elements."
            textToSpeech.speak(msg)
            msg
        } catch (e: Exception) {
            Log.e(TAG, "Error highlighting screen", e)
            val msg = "Unable to highlight screen elements: ${e.message}"
            textToSpeech.speak(msg)
            msg
        }
    }

    private fun handleDescribeScreen(): String {
        return try {
            val bitmap = screenCapture.captureNow()
            val analysis = analyzer.analyzeScreen(bitmap)
            val desc = screenReader.buildScreenDescription(analysis)
            textToSpeech.speak(desc)
            desc
        } catch (e: Exception) {
            Log.e(TAG, "Error describing screen", e)
            val msg = "Could not describe screen content."
            textToSpeech.speak(msg)
            msg
        }
    }

    private fun handleWhatCanYouSee(): String {
        return try {
            val bitmap = screenCapture.captureNow()
            val analysis = analyzer.analyzeScreen(bitmap)

            val reply = if (analysis.elements.isNotEmpty()) {
                "I see ${analysis.elements.size} elements on your screen, including ${analysis.textCount} text blocks and ${analysis.clickableCount} clickable items."
            } else {
                "I don't see distinct UI elements on the current display."
            }
            textToSpeech.speak(reply)
            reply
        } catch (e: Exception) {
            val msg = "I cannot see the screen right now. Please verify screen capture and accessibility permissions."
            textToSpeech.speak(msg)
            msg
        }
    }
}
