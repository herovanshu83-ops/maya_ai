package com.example.assistant.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.max

/**
 * High-performance, low-latency audio capture pipeline for MAYA.
 * Captures 16kHz 16-bit PCM audio, streams audio buffers, and calculates
 * real-time speech amplitude for visualization and wake-word processing.
 */
class AudioPipeline(
    private val context: Context
) {
    companion object {
        private const val TAG = "AudioPipeline"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BUFFER_SIZE = 1024

        @Volatile
        private var instance: AudioPipeline? = null

        fun getInstance(context: Context): AudioPipeline {
            return instance ?: synchronized(this) {
                instance ?: AudioPipeline(context.applicationContext).also { instance = it }
            }
        }
    }

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording = false
    private var audioHandler: Handler? = null
    private var audioThread: HandlerThread? = null

    private var audioCallback: ((ByteArray) -> Unit)? = null
    private var amplitudeCallback: ((Float) -> Unit)? = null

    @Volatile
    private var currentAmplitude = 0f

    fun setAmplitudeCallback(callback: ((Float) -> Unit)?) {
        this.amplitudeCallback = callback
    }

    @Synchronized
    fun startListening(callback: (ByteArray) -> Unit): Boolean {
        if (isRecording) {
            stopListening()
        }

        try {
            // Check microphone permission
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "RECORD_AUDIO permission not granted")
                return false
            }

            audioCallback = callback

            // Get minimum buffer size
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )

            if (minBufferSize <= 0) {
                Log.e(TAG, "Invalid minBufferSize: $minBufferSize")
                return false
            }

            // Create AudioRecord with safe buffer allocation
            val recordBufferSize = max(minBufferSize * 2, BUFFER_SIZE * 4)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                recordBufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            // Start recording
            audioRecord?.startRecording()
            isRecording = true

            // Start processing thread with high audio priority
            audioThread = HandlerThread("MayaAudioThread", Process.THREAD_PRIORITY_URGENT_AUDIO).apply {
                start()
            }
            audioHandler = Handler(audioThread!!.looper)
            audioHandler?.post(processAudioRunnable)

            Log.d(TAG, "AudioPipeline started successfully at ${SAMPLE_RATE}Hz")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Exception starting AudioPipeline: ${e.message}", e)
            stopListening()
            return false
        }
    }

    private val processAudioRunnable = object : Runnable {
        override fun run() {
            if (!isRecording) return

            try {
                val buffer = ByteArray(BUFFER_SIZE)
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                if (read > 0) {
                    // Calculate normalized amplitude (0.0 to 1.0)
                    currentAmplitude = calculateAmplitude(buffer, read)
                    amplitudeCallback?.invoke(currentAmplitude)

                    // Send audio data to callback
                    val copy = buffer.copyOf(read)
                    audioCallback?.invoke(copy)
                }

                if (isRecording) {
                    audioHandler?.post(this)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing audio buffer: ${e.message}", e)
                if (isRecording) {
                    audioHandler?.postDelayed(this, 100)
                }
            }
        }
    }

    private fun calculateAmplitude(buffer: ByteArray, read: Int): Float {
        var maxSample = 0
        var i = 0
        while (i < read - 1) {
            // 16-bit PCM little endian
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            val signedSample = sample.toShort().toInt()
            val absSample = abs(signedSample)
            if (absSample > maxSample) {
                maxSample = absSample
            }
            i += 2
        }
        return (maxSample / 32767f).coerceIn(0f, 1f)
    }

    @Synchronized
    fun stopListening() {
        isRecording = false
        audioHandler?.removeCallbacks(processAudioRunnable)
        audioThread?.quitSafely()
        audioThread = null
        audioHandler = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord: ${e.message}")
        }

        audioRecord = null
        audioCallback = null
        currentAmplitude = 0f
        amplitudeCallback?.invoke(0f)
        Log.d(TAG, "AudioPipeline stopped")
    }

    fun getAmplitude(): Float = currentAmplitude

    fun isAudioAvailable(): Boolean {
        return try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )
            minBufferSize > 0
        } catch (e: Exception) {
            false
        }
    }

    fun isListening(): Boolean = isRecording
}
