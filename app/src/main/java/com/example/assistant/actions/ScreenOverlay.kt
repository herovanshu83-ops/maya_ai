package com.example.assistant.actions

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScreenOverlay(
    private val context: Context
) {

    private val windowManager: WindowManager? = try {
        context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    } catch (e: Exception) {
        null
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    data class VisualActionFeedback(
        val type: String,
        val text: String = "",
        val bounds: Rect? = null,
        val x: Float = 0f,
        val y: Float = 0f,
        val color: Int = android.graphics.Color.CYAN,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _visualFeedbackFlow = MutableStateFlow<VisualActionFeedback?>(null)
    val visualFeedbackFlow: StateFlow<VisualActionFeedback?> = _visualFeedbackFlow.asStateFlow()

    private val activeViews = mutableListOf<View>()

    fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun showTap(x: Float, y: Float, color: Int = Color.CYAN) {
        _visualFeedbackFlow.value = VisualActionFeedback(type = "TAP", x = x, y = y, color = color)

        if (!canDrawOverlays() || windowManager == null) return

        mainHandler.post {
            try {
                val rippleView = createRippleView(x, y, color)
                windowManager.addView(rippleView, createLayoutParams())
                activeViews.add(rippleView)

                mainHandler.postDelayed({
                    removeOverlayView(rippleView)
                }, 600)
            } catch (e: Exception) {
                // WindowManager overlay fallback
            }
        }
    }

    fun showTapWithText(x: Float, y: Float, text: String, color: Int = Color.CYAN) {
        _visualFeedbackFlow.value = VisualActionFeedback(type = "TAP_LABEL", text = text, x = x, y = y, color = color)

        if (!canDrawOverlays() || windowManager == null) return

        mainHandler.post {
            try {
                val tapView = createTapWithLabelView(x, y, text, color)
                windowManager.addView(tapView, createLayoutParams())
                activeViews.add(tapView)

                mainHandler.postDelayed({
                    removeOverlayView(tapView)
                }, 900)
            } catch (e: Exception) {
                // Graceful fallback
            }
        }
    }

    fun highlightElement(bounds: Rect, color: Int = Color.CYAN) {
        _visualFeedbackFlow.value = VisualActionFeedback(type = "HIGHLIGHT", bounds = bounds, color = color)

        if (!canDrawOverlays() || windowManager == null) return

        mainHandler.post {
            try {
                val highlightView = createHighlightView(bounds, color)
                windowManager.addView(highlightView, createLayoutParams())
                activeViews.add(highlightView)

                mainHandler.postDelayed({
                    removeOverlayView(highlightView)
                }, 1200)
            } catch (e: Exception) {
                // Graceful fallback
            }
        }
    }

    fun highlightSearchResults(text: String, elements: List<ScreenMapper.ScreenElement>) {
        _visualFeedbackFlow.value = VisualActionFeedback(type = "SEARCH_RESULTS", text = "Found: $text")

        if (!canDrawOverlays() || windowManager == null) return

        mainHandler.post {
            elements.forEach { element ->
                try {
                    val highlightView = createHighlightView(element.bounds, Color.YELLOW)
                    windowManager.addView(highlightView, createLayoutParams())
                    activeViews.add(highlightView)

                    val labelView = createLabelView(element.bounds, "🔍 $text")
                    windowManager.addView(labelView, createLayoutParams())
                    activeViews.add(labelView)

                    mainHandler.postDelayed({
                        removeOverlayView(highlightView)
                        removeOverlayView(labelView)
                    }, 1800)
                } catch (e: Exception) {
                    // Ignore transient window manager errors
                }
            }
        }
    }

    fun showActionIndicator(action: String, x: Float, y: Float) {
        _visualFeedbackFlow.value = VisualActionFeedback(type = "ACTION", text = action, x = x, y = y)

        if (!canDrawOverlays() || windowManager == null) return

        mainHandler.post {
            try {
                val indicatorView = createActionIndicatorView(action, x, y)
                windowManager.addView(indicatorView, createLayoutParams())
                activeViews.add(indicatorView)

                mainHandler.postDelayed({
                    removeOverlayView(indicatorView)
                }, 1200)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun showScreenMap(screenMap: ScreenMapper.ScreenMap) {
        _visualFeedbackFlow.value = VisualActionFeedback(
            type = "SCREEN_MAP",
            text = "Mapped ${screenMap.elements.size} elements (${screenMap.clickableElements.size} clickable)"
        )

        if (!canDrawOverlays() || windowManager == null) return

        mainHandler.post {
            screenMap.clickableElements.take(8).forEach { element ->
                try {
                    val highlightView = createHighlightView(element.bounds, Color.CYAN)
                    windowManager.addView(highlightView, createLayoutParams())
                    activeViews.add(highlightView)

                    val label = element.text.ifEmpty { element.contentDescription }.ifEmpty { "Clickable" }
                    val labelView = createLabelView(element.bounds, "👆 ${label.take(15)}")
                    windowManager.addView(labelView, createLayoutParams())
                    activeViews.add(labelView)

                    mainHandler.postDelayed({
                        removeOverlayView(highlightView)
                        removeOverlayView(labelView)
                    }, 2500)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    fun showVoiceOutput(text: String) {
        _visualFeedbackFlow.value = VisualActionFeedback(type = "VOICE_OUTPUT", text = text)

        if (!canDrawOverlays() || windowManager == null) return

        mainHandler.post {
            try {
                val voiceView = createVoiceOutputView(text)
                windowManager.addView(voiceView, createVoiceLayoutParams())
                activeViews.add(voiceView)

                mainHandler.postDelayed({
                    removeOverlayView(voiceView)
                }, 2200)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun removeOverlayView(view: View) {
        try {
            if (activeViews.contains(view)) {
                windowManager?.removeView(view)
                activeViews.remove(view)
            }
        } catch (e: Exception) {
            // View already removed
        }
    }

    private fun createRippleView(x: Float, y: Float, color: Int): View {
        return object : View(context) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = 6f
                alpha = 220
            }
            private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
                alpha = 100
            }
            private var progress = 0f

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val radius = 15f + progress * 65f
                canvas.drawCircle(width / 2f, height / 2f, radius, fillPaint)
                canvas.drawCircle(width / 2f, height / 2f, radius, paint)

                paint.alpha = (220 * (1 - progress)).toInt().coerceAtLeast(0)
                fillPaint.alpha = (100 * (1 - progress)).toInt().coerceAtLeast(0)

                if (progress < 1f) {
                    progress += 0.08f
                    postInvalidateOnAnimation()
                }
            }
        }.apply {
            layoutParams = ViewGroup.LayoutParams(160, 160)
            translationX = x - 80
            translationY = y - 80
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun createTapWithLabelView(x: Float, y: Float, text: String, color: Int): View {
        return object : View(context) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
                alpha = 220
            }
            private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.WHITE
                textSize = 28f
                typeface = Typeface.DEFAULT_BOLD
                setShadowLayer(4f, 2f, 2f, Color.BLACK)
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                canvas.drawCircle(30f, 30f, 22f, paint)
                canvas.drawText(text, 60f, 38f, textPaint)
            }
        }.apply {
            layoutParams = ViewGroup.LayoutParams((text.length * 20 + 80).coerceAtLeast(140), 60)
            translationX = x
            translationY = y - 40
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun createHighlightView(bounds: Rect, color: Int): View {
        return object : View(context) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = 5f
                alpha = 220
            }
            private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
                alpha = 40
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        }.apply {
            layoutParams = ViewGroup.LayoutParams(bounds.width().coerceAtLeast(40), bounds.height().coerceAtLeast(40))
            translationX = bounds.left.toFloat()
            translationY = bounds.top.toFloat()
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun createLabelView(bounds: Rect, text: String): View {
        return object : View(context) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.WHITE
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                setShadowLayer(4f, 2f, 2f, Color.BLACK)
            }
            private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.argb(180, 15, 23, 42)
                style = Paint.Style.FILL
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
                canvas.drawRoundRect(rect, 12f, 12f, bgPaint)
                canvas.drawText(text, 12f, 32f, paint)
            }
        }.apply {
            layoutParams = ViewGroup.LayoutParams((text.length * 16 + 24).coerceAtLeast(100), 48)
            translationX = bounds.left.toFloat()
            translationY = (bounds.top.toFloat() - 54f).coerceAtLeast(10f)
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun createActionIndicatorView(action: String, x: Float, y: Float): View {
        return object : View(context) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.CYAN
                style = Paint.Style.FILL
                alpha = 220
            }
            private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.WHITE
                textSize = 28f
                typeface = Typeface.DEFAULT_BOLD
                setShadowLayer(4f, 2f, 2f, Color.BLACK)
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                canvas.drawCircle(30f, 30f, 20f, paint)
                canvas.drawText(action, 60f, 38f, textPaint)
            }
        }.apply {
            layoutParams = ViewGroup.LayoutParams((action.length * 20 + 80).coerceAtLeast(140), 60)
            translationX = x
            translationY = y - 40
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun createVoiceOutputView(text: String): View {
        return object : View(context) {
            private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.argb(220, 15, 23, 42)
                style = Paint.Style.FILL
            }
            private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.WHITE
                textSize = 30f
                typeface = Typeface.DEFAULT_BOLD
                setShadowLayer(4f, 2f, 2f, Color.BLACK)
            }
            private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.CYAN
                style = Paint.Style.FILL
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
                canvas.drawRoundRect(rect, 24f, 24f, bgPaint)
                canvas.drawCircle(32f, height / 2f, 12f, dotPaint)
                canvas.drawText(text, 56f, height / 2f + 10f, textPaint)
            }
        }.apply {
            layoutParams = ViewGroup.LayoutParams((text.length * 18 + 90).coerceIn(180, 800), 70)
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
    }

    private fun createVoiceLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 120
        }
    }
}
