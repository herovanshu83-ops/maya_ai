package com.example.assistant.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlin.math.sqrt

/**
 * Lightweight AudioRecord-based keyword spotting and voice activity detector (VAD)
 * for wake-word activation before launching full speech recognition.
 */
class WakeWordSpotter(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit
) {
    private val tag = "WakeWordSpotter"
    private var audioRecord: AudioRecord? = null
    private var isRunning = false
    private var spotterJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var energyThreshold = 300.0

    fun startListening() {
        if (isRunning) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(tag, "Cannot start WakeWordSpotter: RECORD_AUDIO permission not granted")
            return
        }

        isRunning = true
        spotterJob = coroutineScope.launch {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufferSize, sampleRate / 5) // ~200ms buffer

            try {
                val record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(tag, "AudioRecord initialization failed")
                    isRunning = false
                    return@launch
                }

                audioRecord = record
                record.startRecording()
                Log.d(tag, "WakeWordSpotter started listening via AudioRecord")

                val shortBuffer = ShortArray(bufferSize / 2)
                var speechFramesCount = 0

                while (isRunning && isActive) {
                    val readSize = record.read(shortBuffer, 0, shortBuffer.size)
                    if (readSize > 0) {
                        var sum = 0.0
                        for (i in 0 until readSize) {
                            val sample = shortBuffer[i].toDouble()
                            sum += sample * sample
                        }
                        val rms = sqrt(sum / readSize)

                        if (rms > energyThreshold) {
                            speechFramesCount++
                            if (speechFramesCount >= 3) {
                                Log.d(tag, "Wake word / voice activity detected! RMS: $rms")
                                withContext(Dispatchers.Main) {
                                    onWakeWordDetected()
                                }
                                speechFramesCount = 0
                                stopListeningInternal()
                                break
                            }
                        } else {
                            if (speechFramesCount > 0) {
                                speechFramesCount--
                            }
                        }
                    }
                    delay(50)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error in WakeWordSpotter: ${e.message}", e)
            } finally {
                stopListeningInternal()
            }
        }
    }

    private fun stopListeningInternal() {
        isRunning = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // ignore
        }
        audioRecord = null
    }

    fun stopListening() {
        isRunning = false
        spotterJob?.cancel()
        stopListeningInternal()
        Log.d(tag, "WakeWordSpotter stopped")
    }

    fun isSpotting(): Boolean = isRunning
}
