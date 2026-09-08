package com.example.assistant.actions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.example.assistant.services.MayaAccessibilityService

/**
 * Screen capture service using MediaProjection and Accessibility screenshot fallback.
 */
class ScreenCaptureService(
    private val context: Context
) {
    companion object {
        private const val TAG = "ScreenCaptureService"

        @Volatile
        private var instance: ScreenCaptureService? = null

        fun getInstance(context: Context): ScreenCaptureService {
            return instance ?: synchronized(this) {
                instance ?: ScreenCaptureService(context.applicationContext).also { instance = it }
            }
        }
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    @Volatile
    private var isCapturing = false
    @Volatile
    private var latestBitmap: Bitmap? = null

    private var screenCallback: ((Bitmap) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @Synchronized
    fun startCapture(resultCode: Int, data: Intent, callback: ((Bitmap) -> Unit)? = null): Boolean {
        try {
            if (resultCode != Activity.RESULT_OK) {
                Log.w(TAG, "Screen capture permission not granted by user")
                return false
            }

            stopCapture()
            screenCallback = callback

            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                ?: return false

            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            if (mediaProjection == null) {
                Log.e(TAG, "Failed to get MediaProjection")
                return false
            }

            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay?.getRealMetrics(metrics)

            val width = if (metrics.widthPixels > 0) metrics.widthPixels else 1080
            val height = if (metrics.heightPixels > 0) metrics.heightPixels else 1920
            val density = if (metrics.densityDpi > 0) metrics.densityDpi else DisplayMetrics.DENSITY_DEFAULT

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "MayaScreenCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )

            imageReader?.setOnImageAvailableListener({ reader ->
                try {
                    val image = reader.acquireLatestImage()
                    if (image != null) {
                        val bitmap = imageToBitmap(image)
                        image.close()
                        if (bitmap != null) {
                            latestBitmap = bitmap
                            screenCallback?.let { cb ->
                                mainHandler.post { cb(bitmap) }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error acquiring latest image: ${e.message}")
                }
            }, Handler(Looper.getMainLooper()))

            isCapturing = true
            Log.d(TAG, "Screen capture service started successfully")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaProjection: ${e.message}", e)
            stopCapture()
            return false
        }
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        return try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            if (rowPadding == 0) {
                bitmap
            } else {
                Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            }
        } catch (e: Exception) {
            Log.e(TAG, "imageToBitmap error: ${e.message}")
            null
        }
    }

    fun captureNow(): Bitmap? {
        // Return latest live bitmap if capturing
        if (latestBitmap != null) {
            return latestBitmap
        }

        // Try reading immediately from ImageReader
        try {
            val image = imageReader?.acquireLatestImage()
            if (image != null) {
                val bitmap = imageToBitmap(image)
                image.close()
                if (bitmap != null) {
                    latestBitmap = bitmap
                    return bitmap
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "captureNow image acquire failed: ${e.message}")
        }

        return latestBitmap
    }

    @Synchronized
    fun stopCapture() {
        isCapturing = false
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            mediaProjection?.stop()
            mediaProjection = null
            imageReader?.close()
            imageReader = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping capture: ${e.message}")
        }
        screenCallback = null
        latestBitmap = null
        Log.d(TAG, "Screen capture stopped")
    }

    fun isCapturing(): Boolean = isCapturing
}
