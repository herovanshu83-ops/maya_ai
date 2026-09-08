package com.example.assistant.audio

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * Enterprise-grade Continuous Speech-to-Text Engine for MAYA assistant.
 * Supports:
 * - Single-shot Push-to-Talk
 * - Continuous hands-free conversation loop
 * - Wake-Word continuous trigger ("Hey MAYA", "MAYA", "JARVIS")
 * - On-device / Offline recognition (Android 13+)
 * - Automatic error recovery & soft-timeout silence handling
 * - Audio focus & TTS echo suppression
 */
class SpeechRecognizerHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onPartialResult: (String) -> Unit = {},
    onRmsChanged: (Float) -> Unit = {},
    private val onError: (String) -> Unit = {},
    private val onReady: () -> Unit = {},
    private val onBeginningSpeech: () -> Unit = {},
    private val onWakeWordDetected: (trigger: String, command: String) -> Unit = { _, _ -> }
) {

    private val tag = "MayaSpeechEngine"
    private val sessionLock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val rmsChangedCallback: (Float) -> Unit = onRmsChanged
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: Any? = null // AudioFocusRequest on Android O+

    private val wakeWordSpotter = WakeWordSpotter(context) {
        if (config.mode == ContinuousVoiceMode.WAKE_WORD && !isTemporarilyPaused) {
            Log.d(tag, "WakeWordSpotter triggered, starting full recognition...")
            onWakeWordDetected(config.wakeWord.triggers.first(), "")
            startListeningInternal()
        }
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isContinuousActive = false
    private var isTemporarilyPaused = false
    private var consecutiveErrors = 0
    private val maxConsecutiveErrors = 5

    var config: SpeechToTextConfig = SpeechToTextConfig()
        private set

    init {
        runOnMainThread {
            initRecognizer()
        }
    }

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    private fun requestAudioFocus(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val request = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            Log.d(tag, "Audio focus lost temporarily")
                        }
                    }
                    .build()
                audioFocusRequest = request
                audioManager?.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    null,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to request audio focus: ${e.message}")
            true
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = audioFocusRequest as? android.media.AudioFocusRequest
                if (req != null) {
                    audioManager?.abandonAudioFocusRequest(req)
                    audioFocusRequest = null
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to abandon audio focus: ${e.message}")
        }
    }

    private fun initRecognizer() {
        synchronized(sessionLock) {
            try {
                try {
                    speechRecognizer?.destroy()
                } catch (e: Exception) {
                    // Ignore
                }
                speechRecognizer = null

                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Log.e(tag, "Speech recognition is not available on this device")
                    onError("Speech recognition not available")
                    return
                }

                // Android 13+ on-device speech recognizer support
                speechRecognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    config.preferOffline &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
                ) {
                    Log.d(tag, "Using on-device offline SpeechRecognizer")
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    Log.d(tag, "Using system default SpeechRecognizer")
                    SpeechRecognizer.createSpeechRecognizer(context)
                }

                speechRecognizer?.setRecognitionListener(createRecognitionListener())
            } catch (e: Exception) {
                Log.e(tag, "Error initializing SpeechRecognizer: ${e.message}", e)
                onError("Failed to initialize speech recognizer: ${e.message}")
            }
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                consecutiveErrors = 0
                onReady()
            }

            override fun onBeginningOfSpeech() {
                isListening = true
                onBeginningSpeech()
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (isListening && !isTemporarilyPaused) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    rmsChangedCallback(normalized)
                    if (normalized > 0.15f) {
                        onBeginningSpeech()
                    }
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                rmsChangedCallback(0f)
            }

            override fun onError(error: Int) {
                isListening = false
                rmsChangedCallback(0f)

                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission missing"
                    SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Silence timeout"
                    else -> "Recognition error ($error)"
                }

                Log.d(tag, "SpeechRecognizer onError: $error ($errorMsg) - continuous: $isContinuousActive, paused: $isTemporarilyPaused")

                // Handle Continuous Mode Loop
                if (isContinuousActive && !isTemporarilyPaused) {
                    when (error) {
                        // Soft timeouts: user paused speaking or didn't say anything yet
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                        SpeechRecognizer.ERROR_NO_MATCH -> {
                            consecutiveErrors = 0
                            scheduleRestart(delayMs = 150L)
                            return
                        }

                        // Busy / Client glitches: reinitialize recognizer cleanly
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                        SpeechRecognizer.ERROR_CLIENT,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                            consecutiveErrors++
                            if (consecutiveErrors <= maxConsecutiveErrors) {
                                scheduleRecreateAndRestart(delayMs = 400L * consecutiveErrors)
                                return
                            }
                        }

                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            isContinuousActive = false
                            abandonAudioFocus()
                            onError("Microphone permission required. Please enable it in Settings > Apps > MAYA.")
                            return
                        }

                        else -> {
                            consecutiveErrors++
                            if (consecutiveErrors <= maxConsecutiveErrors) {
                                scheduleRestart(delayMs = 500L)
                                return
                            }
                        }
                    }
                }

                // If not continuous or max retries exceeded
                abandonAudioFocus()
                onError(errorMsg)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                rmsChangedCallback(0f)
                consecutiveErrors = 0

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()?.trim() ?: ""

                Log.d(tag, "SpeechRecognizer onResults: '$spokenText'")

                if (spokenText.isNotBlank()) {
                    if (config.mode == ContinuousVoiceMode.WAKE_WORD) {
                        handleWakeWordResult(spokenText)
                    } else {
                        onResult(spokenText)
                    }
                }

                // Restart continuous listening loop if still active
                if (isContinuousActive && !isTemporarilyPaused) {
                    if (config.mode == ContinuousVoiceMode.WAKE_WORD) {
                        wakeWordSpotter.startListening()
                    } else {
                        scheduleRestart(delayMs = config.restartDelayMs)
                    }
                } else {
                    abandonAudioFocus()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull() ?: ""
                if (partial.isNotBlank() && !isTemporarilyPaused) {
                    onBeginningSpeech()
                    onPartialResult(partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun handleWakeWordResult(text: String) {
        val lower = text.lowercase(Locale.getDefault())
        val triggers = config.wakeWord.triggers

        var matchedTrigger: String? = null
        for (trigger in triggers) {
            if (lower.startsWith(trigger)) {
                matchedTrigger = trigger
                break
            } else if (lower.contains(trigger)) {
                matchedTrigger = trigger
                break
            }
        }

        if (matchedTrigger != null) {
            val command = lower.substringAfter(matchedTrigger).trim()
            Log.d(tag, "Wake word matched: '$matchedTrigger', command: '$command'")
            if (command.isNotBlank()) {
                onWakeWordDetected(matchedTrigger, command)
                onResult(command)
            } else {
                onWakeWordDetected(matchedTrigger, "")
                onPartialResult("Listening for command...")
            }
        } else {
            // Wake word not found in this segment; keep listening in wake-word mode
            Log.d(tag, "Wake word not detected in: '$text'")
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isContinuousActive && !isTemporarilyPaused) {
                startListeningInternal()
            }
        }, delayMs)
    }

    private fun scheduleRecreateAndRestart(delayMs: Long) {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isContinuousActive && !isTemporarilyPaused) {
                initRecognizer()
                startListeningInternal()
            }
        }, delayMs)
    }

    /**
     * Start standard listening (or start continuous mode if enabled).
     */
    fun startListening() {
        runOnMainThread {
            try {
                val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.RECORD_AUDIO
                )
                if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.w(tag, "Record audio permission not granted when trying to start listening")
                    onError("MAYA needs Microphone permission to hear your commands. Please allow it in Settings > Apps > MAYA > Permissions.")
                    return@runOnMainThread
                }
                if (config.mode != ContinuousVoiceMode.PUSH_TO_TALK) {
                    isContinuousActive = true
                }
                isTemporarilyPaused = false
                if (config.mode == ContinuousVoiceMode.WAKE_WORD) {
                    wakeWordSpotter.startListening()
                } else {
                    startListeningInternal()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error in startListening: ${e.message}", e)
                onError("Unable to start listening: ${e.message}")
            }
        }
    }

    private fun startListeningInternal() {
        if (speechRecognizer == null) {
            initRecognizer()
        }

        requestAudioFocus()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 800L)

            if (config.preferOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }

        try {
            speechRecognizer?.cancel()
            speechRecognizer?.startListening(intent)
            isListening = true
            Log.d(tag, "Started listening (mode: ${config.mode})")
        } catch (e: Exception) {
            Log.e(tag, "startListeningInternal failed: ${e.message}")
            abandonAudioFocus()
            onError("Unable to start microphone: ${e.message}")
        }
    }

    /**
     * Stop listening immediately and disable continuous loop.
     */
    fun stopListening() {
        runOnMainThread {
            isContinuousActive = false
            isTemporarilyPaused = false
            mainHandler.removeCallbacksAndMessages(null)
            wakeWordSpotter.stopListening()
            abandonAudioFocus()
            try {
                isListening = false
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                rmsChangedCallback(0f)
            } catch (e: Exception) {
                Log.e(tag, "stopListening failed: ${e.message}")
            }
        }
    }

    /**
     * Temporarily pause listening while TTS or assistant is speaking,
     * preventing audio echo / feedback loop.
     */
    fun pauseForAssistantSpeaking() {
        runOnMainThread {
            isTemporarilyPaused = true
            mainHandler.removeCallbacksAndMessages(null)
            wakeWordSpotter.stopListening()
            try {
                isListening = false
                speechRecognizer?.cancel()
                rmsChangedCallback(0f)
                Log.d(tag, "Paused listening for assistant speech")
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    /**
     * Resume listening automatically after assistant speech completes.
     */
    fun resumeAfterAssistantSpeaking(delayMs: Long = 350L) {
        runOnMainThread {
            isTemporarilyPaused = false
            if (isContinuousActive) {
                if (config.mode == ContinuousVoiceMode.WAKE_WORD) {
                    wakeWordSpotter.startListening()
                } else {
                    scheduleRestart(delayMs)
                }
            }
        }
    }

    fun updateConfig(newConfig: SpeechToTextConfig) {
        runOnMainThread {
            val modeChanged = config.mode != newConfig.mode
            val offlineChanged = config.preferOffline != newConfig.preferOffline
            config = newConfig

            if (newConfig.mode == ContinuousVoiceMode.PUSH_TO_TALK) {
                isContinuousActive = false
            } else {
                isContinuousActive = true
            }

            if (offlineChanged) {
                initRecognizer()
            }

            if (isContinuousActive && !isListening && !isTemporarilyPaused) {
                if (newConfig.mode == ContinuousVoiceMode.WAKE_WORD) {
                    wakeWordSpotter.startListening()
                } else {
                    startListeningInternal()
                }
            }
        }
    }

    fun setContinuousMode(mode: ContinuousVoiceMode) {
        updateConfig(config.copy(mode = mode))
    }

    fun setWakeWord(wakeWord: WakeWordOption) {
        updateConfig(config.copy(wakeWord = wakeWord))
    }

    fun setPreferOffline(preferOffline: Boolean) {
        updateConfig(config.copy(preferOffline = preferOffline))
    }

    fun cancel() {
        runOnMainThread {
            isListening = false
            wakeWordSpotter.stopListening()
            speechRecognizer?.cancel()
            rmsChangedCallback(0f)
        }
    }

    fun destroy() {
        runOnMainThread {
            isContinuousActive = false
            isTemporarilyPaused = false
            mainHandler.removeCallbacksAndMessages(null)
            wakeWordSpotter.stopListening()
            abandonAudioFocus()
            try {
                isListening = false
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun isCurrentlyListening(): Boolean = isListening
    fun isContinuousEnabled(): Boolean = isContinuousActive
    fun isDeviceOfflineRecognitionAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    }
}
