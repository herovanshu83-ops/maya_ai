package com.example.assistant.services

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.assistant.actions.ScreenOverlay
import com.example.utils.VersionUtils

class MayaOverlayService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1002
        const val CHANNEL_ID = "maya_overlay_channel"
        const val ACTION_START = "com.example.assistant.START_OVERLAY"
        const val ACTION_STOP = "com.example.assistant.STOP_OVERLAY"
        const val ACTION_UPDATE = "com.example.assistant.UPDATE_OVERLAY"
        const val ACTION_SHOW_ORB = "com.example.assistant.SHOW_ORB"
        const val ACTION_HIDE_ORB = "com.example.assistant.HIDE_ORB"

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, MayaOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MayaOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }

        fun update(context: Context, text: String, type: String = "info", debugText: String = "") {
            val intent = Intent(context, MayaOverlayService::class.java).apply {
                action = ACTION_UPDATE
                putExtra("text", text)
                putExtra("type", type)
                if (debugText.isNotBlank()) {
                    putExtra("debugText", debugText)
                }
            }
            if (isRunning) {
                try {
                    context.startService(intent)
                } catch (e: Exception) {
                    // Ignore if background start restriction
                }
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var floatingOrbView: View? = null
    private var floatingHudView: View? = null
    private var hudTextView: TextView? = null
    private var debugTextView: TextView? = null
    private var isOrbShowing = false
    private var isHudShowing = false
    private var isDebugExpanded = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification("MAYA Overlay HUD Active"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification("MAYA Overlay HUD Active"))
        }

        if (hasOverlayPermission()) {
            showFloatingOrb()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_STOP -> {
                hideAll()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                if (hasOverlayPermission()) {
                    if (!isOrbShowing) showFloatingOrb()
                }
            }
            ACTION_SHOW_ORB -> {
                if (hasOverlayPermission()) showFloatingOrb()
            }
            ACTION_HIDE_ORB -> {
                hideFloatingOrb()
            }
            ACTION_UPDATE -> {
                val text = intent.getStringExtra("text") ?: ""
                val type = intent.getStringExtra("type") ?: "info"
                val debugInfo = intent.getStringExtra("debugText") ?: ""
                if (text.isNotBlank()) {
                    showOrUpdateHud(text, type, debugInfo)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        hideAll()
        super.onDestroy()
    }

    private fun hideAll() {
        hideFloatingOrb()
        hideHud()
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    // ==========================================
    // FLOATING 3D GLOWING ORB
    // ==========================================

    private fun showFloatingOrb() {
        if (isOrbShowing || windowManager == null || !hasOverlayPermission()) return

        val orbSize = (64 * resources.displayMetrics.density).toInt()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            orbSize,
            orbSize,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels - orbSize - 40)
            y = (resources.displayMetrics.heightPixels * 0.4f).toInt()
        }

        floatingOrbView = createOrbFloatingView()

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoving = false

        floatingOrbView?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoving = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isMoving = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager?.updateViewLayout(floatingOrbView, params)
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoving && (event.eventTime - event.downTime < 400)) {
                        // Single tap: open main app / assistant
                        openMainApp()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(floatingOrbView, params)
            isOrbShowing = true
        } catch (e: Exception) {
            // Permission or window error
        }
    }

    private fun hideFloatingOrb() {
        if (floatingOrbView != null && isOrbShowing) {
            try {
                windowManager?.removeView(floatingOrbView)
            } catch (e: Exception) {
                // Ignore
            }
            floatingOrbView = null
            isOrbShowing = false
        }
    }

    private fun createOrbFloatingView(): View {
        return object : View(this) {
            private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#00F0FF")
                style = Paint.Style.STROKE
                strokeWidth = 4f
                alpha = 200
            }
            private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                style = Paint.Style.FILL
            }
            private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#00F0FF")
                style = Paint.Style.FILL
                alpha = 80
            }
            private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 28f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
                setShadowLayer(6f, 0f, 0f, Color.parseColor("#00F0FF"))
            }

            private var pulsePhase = 0f

            override fun onAttachedToWindow() {
                super.onAttachedToWindow()
                val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 2000
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    addUpdateListener {
                        pulsePhase = it.animatedValue as Float
                        postInvalidateOnAnimation()
                    }
                }
                animator.start()
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val cx = width / 2f
                val cy = height / 2f
                val baseRadius = (width.coerceAtMost(height) / 2f) - 8f

                // Outer pulsing aura
                val glowRadius = baseRadius + pulsePhase * 6f
                glowPaint.alpha = (50 + pulsePhase * 70).toInt()
                canvas.drawCircle(cx, cy, glowRadius, glowPaint)

                // Core sphere
                canvas.drawCircle(cx, cy, baseRadius, corePaint)

                // Neon ring
                outerPaint.alpha = (180 + pulsePhase * 75).toInt()
                canvas.drawCircle(cx, cy, baseRadius, outerPaint)

                // Inner Holographic Indicator text
                val fontMetrics = textPaint.fontMetrics
                val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
                canvas.drawText("M", cx, textY, textPaint)
            }
        }
    }

    // ==========================================
    // FLOATING ACTION HUD / VOICE BANNER
    // ==========================================

    private fun showOrUpdateHud(text: String, type: String, debugInfo: String = "") {
        if (!hasOverlayPermission() || windowManager == null) return

        mainHandler.post {
            if (floatingHudView == null) {
                createAndShowHud()
            }
            hudTextView?.text = text
            if (debugInfo.isNotBlank()) {
                debugTextView?.text = debugInfo
            }

            val textColor = when (type.lowercase()) {
                "listening" -> Color.parseColor("#10B981")
                "thinking" -> Color.parseColor("#FACC15")
                "speaking" -> Color.parseColor("#00F0FF")
                "error" -> Color.parseColor("#EF4444")
                "success" -> Color.parseColor("#10B981")
                else -> Color.WHITE
            }
            hudTextView?.setTextColor(textColor)

            // Auto-hide HUD text banner after 4 seconds unless continuous or debug shown
            mainHandler.removeCallbacks(autoHideHudRunnable)
            if (!isDebugExpanded) {
                mainHandler.postDelayed(autoHideHudRunnable, 4500)
            }
        }
    }

    private val autoHideHudRunnable = Runnable {
        hideHud()
    }

    private fun createAndShowHud() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100
        }

        val hudContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 18, 24, 18)
            gravity = Gravity.START
            background = createHudBackground()
            elevation = 16f
        }

        // Header Row: Dot + MAYA Brand + Status
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val dot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(18, 18).apply {
                marginEnd = 12
            }
            background = createDotBackground()
        }

        val brandText = TextView(this).apply {
            text = "MAYA"
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
        }

        val separator = TextView(this).apply {
            text = " • "
            setTextColor(Color.parseColor("#64748B"))
            textSize = 12f
        }

        val statusText = TextView(this).apply {
            text = "Active"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
        }

        headerRow.addView(dot)
        headerRow.addView(brandText)
        headerRow.addView(separator)
        headerRow.addView(statusText)

        // Main action / progress text
        val textView = TextView(this).apply {
            text = "MAYA Ready"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 3
            maxWidth = (resources.displayMetrics.widthPixels * 0.82f).toInt()
            setPadding(0, 6, 0, 8)
        }
        hudTextView = textView

        // Action Buttons Row (Stop, Retry, Open)
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 4, 0, 0)
        }

        val stopButton = TextView(this).apply {
            text = "⏹ Stop"
            setTextColor(Color.parseColor("#EF4444"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(16, 8, 16, 8)
            background = createButtonBackground(Color.parseColor("#33EF4444"))
            setOnClickListener {
                com.example.assistant.core.TaskEngine.getInstance(this@MayaOverlayService).cancelCurrentTask("Stopped via HUD")
            }
        }

        val retryButton = TextView(this).apply {
            text = "🔄 Retry"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 12
                marginEnd = 12
            }
            setPadding(16, 8, 16, 8)
            background = createButtonBackground(Color.parseColor("#3338BDF8"))
            setOnClickListener {
                com.example.assistant.core.TaskEngine.getInstance(this@MayaOverlayService).retryCurrentStep()
            }
        }

        val openAppButton = TextView(this).apply {
            text = "↗ App"
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 12f
            setPadding(16, 8, 16, 8)
            background = createButtonBackground(Color.parseColor("#3364748B"))
            setOnClickListener {
                openMainApp()
            }
        }

        val debugButton = TextView(this).apply {
            text = "⚡ Debug"
            setTextColor(Color.parseColor("#FACC15"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 12
            }
            setPadding(16, 8, 16, 8)
            background = createButtonBackground(Color.parseColor("#33FACC15"))
            setOnClickListener {
                isDebugExpanded = !isDebugExpanded
                debugTextView?.visibility = if (isDebugExpanded) View.VISIBLE else View.GONE
            }
        }

        buttonRow.addView(stopButton)
        buttonRow.addView(retryButton)
        buttonRow.addView(openAppButton)
        buttonRow.addView(debugButton)

        // MAYA DEBUG block (Requirement 18)
        val debugView = TextView(this).apply {
            setTextColor(Color.parseColor("#E2E8F0"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setPadding(16, 12, 16, 12)
            background = createButtonBackground(Color.parseColor("#40000000"))
            visibility = if (isDebugExpanded) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 12
            }
        }
        debugTextView = debugView

        hudContainer.addView(headerRow)
        hudContainer.addView(textView)
        hudContainer.addView(buttonRow)
        hudContainer.addView(debugView)

        floatingHudView = hudContainer

        try {
            windowManager?.addView(floatingHudView, params)
            isHudShowing = true
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun hideHud() {
        if (floatingHudView != null && isHudShowing) {
            try {
                windowManager?.removeView(floatingHudView)
            } catch (e: Exception) {
                // Ignore
            }
            floatingHudView = null
            isHudShowing = false
        }
    }

    private fun createHudBackground(): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 32f
            setColor(Color.parseColor("#E60F172A"))
            setStroke(2, Color.parseColor("#4D00F0FF"))
        }
    }

    private fun createDotBackground(): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(Color.parseColor("#00F0FF"))
        }
    }

    private fun createButtonBackground(fillColor: Int): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 16f
            setColor(fillColor)
        }
    }

    private fun openMainApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MAYA Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating HUD and visual feedback on screen"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, MayaOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MAYA Floating Overlay")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_maya)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close Overlay", stopPendingIntent)
            .build()
    }
}
