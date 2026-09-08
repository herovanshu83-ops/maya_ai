package com.example.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.assistant.services.MayaForegroundService
import com.example.assistant.services.MayaOverlayService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = context.getSharedPreferences("maya_prefs", Context.MODE_PRIVATE)
            val autoStart = prefs.getBoolean("auto_start_background", true)
            val autoOverlay = prefs.getBoolean("auto_start_overlay", false)

            if (autoStart) {
                MayaForegroundService.start(context)
            }
            if (autoOverlay && OverlayPermissionHelper(context).hasOverlayPermission()) {
                MayaOverlayService.start(context)
            }
        }
    }
}
