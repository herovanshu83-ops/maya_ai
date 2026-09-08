package com.example.assistant.actions

import android.content.Context
import android.util.Log
import com.example.assistant.audio.TextToSpeechHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Screen Reader.
 * Reads out loud the visual contents of the current Android screen using TextToSpeech,
 * finds requested elements by name or content description, and formats natural language descriptions.
 */
class ScreenReader(
    private val context: Context,
    private val screenCapture: ScreenCaptureService = ScreenCaptureService.getInstance(context),
    private val screenAnalyzer: ScreenAnalyzer = ScreenAnalyzer(context),
    private val textToSpeech: TextToSpeechHelper = TextToSpeechHelper.getInstance(context)
) {
    companion object {
        private const val TAG = "ScreenReader"

        @Volatile
        private var instance: ScreenReader? = null

        fun getInstance(context: Context): ScreenReader {
            return instance ?: synchronized(this) {
                instance ?: ScreenReader(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    @Volatile
    private var isReading = false

    fun readScreen(): Boolean {
        if (isReading) return false

        try {
            isReading = true
            val bitmap = screenCapture.captureNow()
            val analysis = screenAnalyzer.analyzeScreen(bitmap)
            val description = buildScreenDescription(analysis)

            textToSpeech.speak(description)
            isReading = false
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error in readScreen: ${e.message}", e)
            textToSpeech.speak("Unable to read screen.")
            isReading = false
            return false
        }
    }

    fun readScreenAsync(onComplete: ((String) -> Unit)? = null): Boolean {
        if (isReading) return false

        isReading = true
        scope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    screenCapture.captureNow()
                }

                val analysis = withContext(Dispatchers.Default) {
                    screenAnalyzer.analyzeScreen(bitmap)
                }

                val description = buildScreenDescription(analysis)

                withContext(Dispatchers.Main) {
                    textToSpeech.speak(description)
                    isReading = false
                    onComplete?.invoke(description)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in readScreenAsync: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    textToSpeech.speak("I encountered an issue reading the screen.")
                    isReading = false
                    onComplete?.invoke("Error reading screen")
                }
            }
        }
        return true
    }

    fun findElementOnScreen(queryText: String): ScreenAnalyzer.ScreenElement? {
        return try {
            val bitmap = screenCapture.captureNow()
            val analysis = screenAnalyzer.analyzeScreen(bitmap)
            analysis.elements.find {
                it.text.contains(queryText, ignoreCase = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding element: ${e.message}")
            null
        }
    }

    fun findAndSpeak(queryText: String): Boolean {
        val element = findElementOnScreen(queryText)
        return if (element != null) {
            val label = if (element.text.isNotBlank()) element.text else element.type
            val bounds = element.bounds
            val spoken = "Found \"$label\" at position left ${bounds.left}, top ${bounds.top}."
            textToSpeech.speak(spoken)
            true
        } else {
            textToSpeech.speak("Could not find \"$queryText\" on the screen.")
            false
        }
    }

    fun buildScreenDescription(analysis: ScreenAnalyzer.ScreenAnalysis): String {
        val parts = mutableListOf<String>()

        parts.add(analysis.summary)

        val prominentTexts = analysis.elements
            .filter { it.type == "text" && it.text.isNotBlank() }
            .take(5)
            .map { it.text }

        if (prominentTexts.isNotEmpty()) {
            parts.add("Main content includes: ${prominentTexts.joinToString(", ")}")
        }

        if (analysis.hasButtons) {
            val namedButtons = analysis.elements
                .filter { it.type == "button" && it.text.isNotBlank() }
                .map { it.text }

            if (namedButtons.isNotEmpty()) {
                parts.add("Buttons: ${namedButtons.joinToString(", ")}")
            } else {
                parts.add("${analysis.clickableCount} clickable items available.")
            }
        }

        if (analysis.hasInputs) {
            parts.add("${analysis.elements.count { it.type == "input" }} input fields ready for typing.")
        }

        return parts.joinToString(". ")
    }

    fun stopReading() {
        isReading = false
        textToSpeech.stop()
    }
}
