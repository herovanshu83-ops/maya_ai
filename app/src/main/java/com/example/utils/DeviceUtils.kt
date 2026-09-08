package com.example.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.widget.Toast
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

object DeviceUtils {

    data class BatteryInfo(
        val percentage: Int,
        val isCharging: Boolean,
        val statusText: String
    )

    data class MemoryInfo(
        val totalRamMb: Long,
        val freeRamMb: Long,
        val usedRamMb: Long,
        val percentUsed: Int
    )

    data class StorageInfo(
        val totalGb: Double,
        val freeGb: Double,
        val usedGb: Double,
        val percentUsed: Int
    )

    fun getBatteryInfo(context: Context): BatteryInfo {
        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, intentFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

            val percentage = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 100
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val statusText = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_FULL -> "Fully Charged"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                else -> "Active"
            }

            BatteryInfo(percentage, isCharging, statusText)
        } catch (e: Exception) {
            BatteryInfo(100, false, "Normal")
        }
    }

    fun getMemoryInfo(context: Context): MemoryInfo {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)
            val total = memInfo.totalMem / (1024 * 1024)
            val avail = memInfo.availMem / (1024 * 1024)
            val used = total - avail
            val pct = if (total > 0) ((used * 100) / total).toInt() else 0
            MemoryInfo(total, avail, used, pct)
        } catch (e: Exception) {
            MemoryInfo(8192, 4096, 4096, 50)
        }
    }

    fun getStorageInfo(): StorageInfo {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - freeBytes

            val totalGb = String.format("%.1f", totalBytes / (1024.0 * 1024.0 * 1024.0)).toDouble()
            val freeGb = String.format("%.1f", freeBytes / (1024.0 * 1024.0 * 1024.0)).toDouble()
            val usedGb = String.format("%.1f", usedBytes / (1024.0 * 1024.0 * 1024.0)).toDouble()
            val pct = if (totalGb > 0) ((usedGb * 100) / totalGb).toInt() else 0

            StorageInfo(totalGb, freeGb, usedGb, pct)
        } catch (e: Exception) {
            StorageInfo(128.0, 64.0, 64.0, 50)
        }
    }

    private var isTorchOn = false

    fun toggleFlashlight(context: Context, state: Boolean? = null): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager?.cameraIdList?.firstOrNull()

            if (cameraId != null) {
                val targetState = state ?: !isTorchOn
                cameraManager?.setTorchMode(cameraId, targetState)
                isTorchOn = targetState
                isTorchOn
            } else {
                false
            }
        } catch (e: CameraAccessException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    fun isFlashlightOn(): Boolean = isTorchOn

    fun setVolume(context: Context, percentage: Int, stream: Int = AudioManager.STREAM_MUSIC): String {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "Audio service unavailable"
            val maxVol = audioManager.getStreamMaxVolume(stream)
            val target = ((percentage.coerceIn(0, 100) / 100f) * maxVol).toInt()
            audioManager.setStreamVolume(stream, target, AudioManager.FLAG_SHOW_UI)
            "Volume set to $percentage%"
        } catch (e: Exception) {
            "Unable to adjust volume: ${e.message}"
        }
    }

    fun adjustVolume(context: Context, increase: Boolean, stream: Int = AudioManager.STREAM_MUSIC): String {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "Audio service unavailable"
            val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            audioManager.adjustStreamVolume(stream, direction, AudioManager.FLAG_SHOW_UI)
            if (increase) "Volume increased" else "Volume decreased"
        } catch (e: Exception) {
            "Unable to change volume"
        }
    }

    fun muteVolume(context: Context, mute: Boolean): String {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "Audio service unavailable"
            if (mute) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                "Media muted"
            } else {
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max / 2, AudioManager.FLAG_SHOW_UI)
                "Media unmuted"
            }
        } catch (e: Exception) {
            "Unable to toggle mute"
        }
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "Unavailable"
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return "127.0.0.1"
    }

    fun isNetworkConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun openWifiSettings(context: Context) {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openBluetoothSettings(context: Context) {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openDisplaySettings(context: Context) {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openSoundSettings(context: Context) {
        val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openBatterySettings(context: Context) {
        val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(fallback)
        }
    }
}
