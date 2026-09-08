package com.example.assistant.actions

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager

/**
 * Screen Highlight Overlay.
 * Renders glowing cyan boundary boxes and tags over detected screen elements
 * using WindowManager system overlay view.
 */
class ScreenHighlightOverlay(
    private val context: Context
) {
    companion object {
        private const val TAG = "ScreenHighlightOverlay"

        @Volatile
        private var instance: ScreenHighlightOverlay? = null

        fun getInstance(context: Context): ScreenHighlightOverlay {
            return instance ?: synchronized(this) {
                instance ?: ScreenHighlightOverlay(context.applicationContext).also { instance = it }
            }
        }
    }

    private val windowManager: WindowManager? = try {
        context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    } catch (e: Exception) {
        null
    }

    private var overlayView: View? = null
    @Volatile
    private var isShowing = false
    private val mainHandler = Handler(Looper.getMainLooper())

    fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun showHighlights(elements: List<ScreenAnalyzer.ScreenElement>) {
        if (!canDrawOverlays() || windowManager == null) {
            Log.w(TAG, "Cannot draw overlay: permission missing or WindowManager null")
            return
        }

        mainHandler.post {
            if (isShowing) {
                hideHighlights()
            }

            try {
                val boxStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#00F0FF")
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                }

                val boxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#00F0FF")
                    style = Paint.Style.FILL
                    alpha = 45
                }

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 32f
                    setShadowLayer(6f, 2f, 2f, Color.BLACK)
                }

                val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#0F172A")
                    style = Paint.Style.FILL
                    alpha = 210
                }

                overlayView = object : View(context) {
                    override fun onDraw(canvas: Canvas) {
                        super.onDraw(canvas)

                        elements.forEach { elem ->
                            val bounds = elem.bounds
                            if (bounds.width() <= 0 || bounds.height() <= 0) return@forEach

                            val rectF = RectF(
                                bounds.left.toFloat(),
                                bounds.top.toFloat(),
                                bounds.right.toFloat(),
                                bounds.bottom.toFloat()
                            )

                            canvas.drawRoundRect(rectF, 12f, 12f, boxFillPaint)
                            canvas.drawRoundRect(rectF, 12f, 12f, boxStrokePaint)

                            val labelText = when {
                                elem.text.isNotBlank() -> elem.text.take(24)
                                else -> elem.type.uppercase()
                            }

                            val textWidth = textPaint.measureText(labelText)
                            val labelTop = (bounds.top - 42f).coerceAtLeast(10f)
                            val labelRect = RectF(
                                bounds.left.toFloat(),
                                labelTop,
                                bounds.left.toFloat() + textWidth + 24f,
                                labelTop + 38f
                            )

                            canvas.drawRoundRect(labelRect, 8f, 8f, labelBgPaint)
                            canvas.drawText(
                                labelText,
                                bounds.left.toFloat() + 12f,
                                labelTop + 28f,
                                textPaint
                            )
                        }
                    }
                }

                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                )

                windowManager.addView(overlayView, params)
                isShowing = true
                Log.d(TAG, "Highlights rendered for ${elements.size} elements")

                // Auto-dismiss highlights after 6 seconds to avoid blocking the screen permanently
                mainHandler.postDelayed({
                    if (isShowing) {
                        hideHighlights()
                    }
                }, 6000L)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to show highlights overlay: ${e.message}", e)
                isShowing = false
                overlayView = null
            }
        }
    }

    fun hideHighlights() {
        mainHandler.post {
            if (isShowing && overlayView != null && windowManager != null) {
                try {
                    windowManager.removeView(overlayView)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to remove overlayView: ${e.message}")
                }
                overlayView = null
                isShowing = false
                Log.d(TAG, "Highlights overlay hidden")
            }
        }
    }

    fun isShowing(): Boolean = isShowing
}
