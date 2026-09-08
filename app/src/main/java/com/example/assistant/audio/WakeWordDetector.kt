package com.example.assistant.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.math.sqrt

/**
 * Continuous wake-word & voice energy detector for MAYA.
 * Analyzes audio frames from AudioPipeline to spot wake triggers ("Hey Maya")
 * and speech activity.
 */
class WakeWordDetector(
    private val context: Context,
    private val audioPipeline: AudioPipeline = AudioPipeline.getInstance(context)
) {
    companion object {
        private const val TAG = "WakeWordDetector"
        const val WAKE_WORD = "hey maya"
        private const val ENERGY_THRESHOLD = 3800.0
        private const val TRIGGER_COOLDOWN_MS = 2500L
    }

    private var wakeCallback: (() -> Unit)? = null
    @Volatile
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var consecutiveSpeechFrames = 0
    private var lastTriggerTime = 0L

    @Synchronized
    fun start(callback: () -> Unit): Boolean {
        if (isRunning) {
            return true
        }

        try {
            // Check microphone permission
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Cannot start WakeWordDetector: RECORD_AUDIO permission missing")
                return false
            }

            wakeCallback = callback
            isRunning = true
            consecutiveSpeechFrames = 0

            val started = audioPipeline.startListening { audioData ->
                detectWakeWord(audioData)
            }

            if (!started) {
                isRunning = false
                return false
            }

            Log.d(TAG, "WakeWordDetector successfully started")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WakeWordDetector: ${e.message}", e)
            isRunning = false
            return false
        }
    }

    private fun detectWakeWord(audioData: ByteArray) {
        if (!isRunning) return

        try {
            val sampleCount = audioData.size / 2
            if (sampleCount <= 0) return

            var sumSquares = 0.0
            for (i in 0 until sampleCount) {
                val byteLow = audioData[i * 2].toInt() and 0xFF
                val byteHigh = audioData[i * 2 + 1].toInt() shl 8
                val sample = (byteLow or byteHigh).toShort().toDouble()
                sumSquares += sample * sample
            }

            val avgEnergy = sumSquares / sampleCount
            val rms = sqrt(avgEnergy)

            val now = System.currentTimeMillis()
            if (avgEnergy > ENERGY_THRESHOLD || rms > 65.0) {
                consecutiveSpeechFrames++
                if (consecutiveSpeechFrames >= 2 && (now - lastTriggerTime > TRIGGER_COOLDOWN_MS)) {
                    lastTriggerTime = now
                    consecutiveSpeechFrames = 0
                    Log.d(TAG, "Voice / WakeWord detected! RMS: $rms, Energy: $avgEnergy")
                    handler.post {
                        wakeCallback?.invoke()
                    }
                }
            } else {
                if (consecutiveSpeechFrames > 0) {
                    consecutiveSpeechFrames--
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in detectWakeWord: ${e.message}")
        }
    }

    @Synchronized
    fun stop() {
        isRunning = false
        audioPipeline.stopListening()
        wakeCallback = null
        consecutiveSpeechFrames = 0
        Log.d(TAG, "WakeWordDetector stopped")
    }

    fun isRunning(): Boolean = isRunning
}
