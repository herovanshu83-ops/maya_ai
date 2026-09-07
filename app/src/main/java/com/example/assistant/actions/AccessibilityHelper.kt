package com.example.assistant.actions

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import com.example.assistant.services.MayaAccessibilityService
import kotlinx.coroutines.delay

class AccessibilityHelper(private val context: Context) {

    fun isServiceEnabled(): Boolean {
        return MayaAccessibilityService.isServiceActive(context)
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    suspend fun performYouTubeSearch(query: String): Boolean {
        val service = MayaAccessibilityService.instance ?: return false
        delay(600)

        // Try clicking search button/icon by content description, text, or view ID
        val tappedSearch = service.findAndTapByDescription("Search") ||
                service.findAndTapByDescription("Search YouTube") ||
                service.findAndTapByText("Search") ||
                service.findAndTapByViewId("menu_item_search") ||
                service.findAndTapByViewId("search_edit_text")

        delay(600)
        // Type search query
        service.typeText(query)
        delay(500)

        // Try clicking search keyboard action or search icon
        service.findAndTapByDescription("Search") ||
                service.findAndTapByText("Search") ||
                service.findAndTapByViewId("search_button")

        return true
    }

    suspend fun clickFirstVideo(): Boolean {
        val service = MayaAccessibilityService.instance ?: return false
        delay(1200)

        // Click first video element or thumbnail
        return service.findAndTapByClass("android.widget.ImageView", 1) ||
                service.findAndTapByClass("android.view.ViewGroup", 2) ||
                service.findAndTapByClass("android.view.View", 3)
    }

    suspend fun performSpotifySearch(query: String): Boolean {
        val service = MayaAccessibilityService.instance ?: return false
        delay(600)

        service.findAndTapByText("Search") ||
                service.findAndTapByDescription("Search")

        delay(600)
        service.typeText(query)
        delay(600)
        return true
    }

    suspend fun clickFirstSpotifyTrack(): Boolean {
        val service = MayaAccessibilityService.instance ?: return false
        delay(1200)

        return service.findAndTapByClass("android.view.ViewGroup", 1) ||
                service.findAndTapByClass("android.widget.TextView", 2) ||
                service.findAndTapByClass("android.view.View", 2)
    }

    suspend fun performInstagramSearch(query: String): Boolean {
        val service = MayaAccessibilityService.instance ?: return false
        delay(600)

        service.findAndTapByDescription("Search and explore") ||
                service.findAndTapByText("Search") ||
                service.findAndTapByDescription("Search")

        delay(600)
        service.typeText(query)
        delay(600)
        return true
    }

    suspend fun performWhatsAppSearch(query: String): Boolean {
        val service = MayaAccessibilityService.instance ?: return false
        delay(600)

        service.findAndTapByDescription("Search") ||
                service.findAndTapByText("Search") ||
                service.findAndTapByViewId("menuitem_search")

        delay(600)
        service.typeText(query)
        delay(600)
        return true
    }

    suspend fun performAppSearch(query: String): Boolean {
        val service = MayaAccessibilityService.instance ?: return false
        delay(600)

        service.findAndTapByDescription("Search") ||
                service.findAndTapByText("Search") ||
                service.findAndTapByViewId("search") ||
                service.findAndTapByClass("android.widget.EditText", 0)

        delay(600)
        service.typeText(query)
        delay(600)
        return true
    }

    suspend fun toggleWifi(turnOn: Boolean? = null): Boolean {
        val service = MayaAccessibilityService.instance
        if (service != null) {
            // Open quick settings panel and toggle Wi-Fi tile
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            delay(800)

            val toggled = service.findAndTapByText("Wi-Fi") ||
                    service.findAndTapByText("Internet") ||
                    service.findAndTapByDescription("Wi-Fi") ||
                    service.findAndTapByDescription("Internet")

            if (toggled) {
                delay(600)
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                return true
            }
        }

        // Fallback: launch Wi-Fi settings
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    suspend fun toggleBluetooth(turnOn: Boolean? = null): Boolean {
        val service = MayaAccessibilityService.instance
        if (service != null) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            delay(800)

            val toggled = service.findAndTapByText("Bluetooth") ||
                    service.findAndTapByDescription("Bluetooth")

            if (toggled) {
                delay(600)
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                return true
            }
        }

        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    fun setAlarm(hour: Int, minute: Int, message: String = "MAYA Alarm"): Boolean {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            val fallback = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
            false
        }
    }
}
