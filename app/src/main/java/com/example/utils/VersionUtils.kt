package com.example.utils

import android.Manifest
import android.os.Build

object VersionUtils {
    val sdkInt: Int get() = Build.VERSION.SDK_INT

    val isAndroid8: Boolean = sdkInt >= Build.VERSION_CODES.O // API 26
    val isAndroid8_1: Boolean = sdkInt >= Build.VERSION_CODES.O_MR1 // API 27
    val isAndroid9: Boolean = sdkInt >= Build.VERSION_CODES.P // API 28
    val isAndroid10: Boolean = sdkInt >= Build.VERSION_CODES.Q // API 29
    val isAndroid11: Boolean = sdkInt >= Build.VERSION_CODES.R // API 30
    val isAndroid12: Boolean = sdkInt >= Build.VERSION_CODES.S // API 31
    val isAndroid12L: Boolean = sdkInt >= Build.VERSION_CODES.S_V2 // API 32
    val isAndroid13: Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU // API 33
    val isAndroid14: Boolean = sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE // API 34
    val isAndroid15: Boolean = sdkInt >= 35 // API 35
    val isAndroid16: Boolean = sdkInt >= 36 // API 36

    val osCodename: String
        get() = when {
            sdkInt >= 36 -> "Android 16 (Baklava)"
            sdkInt == 35 -> "Android 15 (Vanilla Ice Cream)"
            sdkInt == 34 -> "Android 14 (Upside Down Cake)"
            sdkInt == 33 -> "Android 13 (Tiramisu)"
            sdkInt == 32 -> "Android 12L (Snow Cone v2)"
            sdkInt == 31 -> "Android 12 (Snow Cone)"
            sdkInt == 30 -> "Android 11 (Red Velvet Cake)"
            sdkInt == 29 -> "Android 10 (Queen Cake)"
            sdkInt == 28 -> "Android 9 (Pie)"
            sdkInt == 27 -> "Android 8.1 (Oreo MR1)"
            sdkInt == 26 -> "Android 8.0 (Oreo)"
            else -> "Android API $sdkInt"
        }

    fun getStoragePermission(): String {
        return if (isAndroid13) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun getBluetoothPermission(): String {
        return if (isAndroid12) {
            Manifest.permission.BLUETOOTH_CONNECT
        } else {
            Manifest.permission.BLUETOOTH
        }
    }

    fun getNotificationPermission(): String? {
        return if (isAndroid13) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
    }

    fun isScopedStorage(): Boolean = isAndroid10
    fun supportsDynamicColor(): Boolean = isAndroid12
    fun supportsPhotoPicker(): Boolean = isAndroid13
}
