package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.audio.ContinuousVoiceMode
import com.example.assistant.audio.WakeWordOption
import com.example.assistant.core.OrbTheme
import com.example.utils.OverlayPermissionHelper
import com.example.utils.VersionUtils

@Composable
fun SettingsScreen(
    currentOrbTheme: OrbTheme,
    onSelectOrbTheme: (OrbTheme) -> Unit,
    ttsSpeed: Float,
    onTtsSpeedChange: (Float) -> Unit,
    isContinuousMode: Boolean,
    onContinuousModeToggle: (Boolean) -> Unit,
    isBackgroundServiceRunning: Boolean,
    onToggleBackgroundService: () -> Unit,
    isOverlayRunning: Boolean,
    onToggleOverlayService: () -> Unit,
    isAutoStartEnabled: Boolean,
    onToggleAutoStart: () -> Unit,
    continuousVoiceMode: ContinuousVoiceMode = ContinuousVoiceMode.ALWAYS_ON,
    onContinuousVoiceModeChange: (ContinuousVoiceMode) -> Unit = {},
    wakeWordOption: WakeWordOption = WakeWordOption.MAYA,
    onWakeWordChange: (WakeWordOption) -> Unit = {},
    preferOfflineSpeech: Boolean = true,
    onPreferOfflineSpeechChange: (Boolean) -> Unit = {},
    isDeviceOfflineAvailable: Boolean = false,
    onOpenApiKeys: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val overlayHelper = remember { OverlayPermissionHelper(context) }
    var hasOverlayPermission by remember { mutableStateOf(overlayHelper.hasOverlayPermission()) }
    var hasBatteryExemption by remember { mutableStateOf(overlayHelper.hasBatteryOptimizationPermission()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Assistant Settings",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Configure background voice service, floating HUD, and system permissions",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        }

        // Background & Overlay System Controls
        item {
            Text(
                text = "Background & System Overlay",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF00F0FF)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Background Service
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Background Voice Engine", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isBackgroundServiceRunning) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF64748B).copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isBackgroundServiceRunning) "ACTIVE" else "STOPPED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBackgroundServiceRunning) Color(0xFF10B981) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                            Text("Keep listening and executing actions when app is closed", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        }
                        Switch(
                            checked = isBackgroundServiceRunning,
                            onCheckedChange = { onToggleBackgroundService() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00F0FF)
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    // Floating System Overlay Orb
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Floating 3D Orb & HUD", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isOverlayRunning) Color(0xFF00F0FF).copy(alpha = 0.2f) else Color(0xFF64748B).copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isOverlayRunning) "VISIBLE" else "OFF",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOverlayRunning) Color(0xFF00F0FF) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                            Text("Display floating glowing orb & live voice feedback over any app", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        }
                        Switch(
                            checked = isOverlayRunning,
                            onCheckedChange = {
                                if (!hasOverlayPermission) {
                                    (context as? android.app.Activity)?.let { act ->
                                        overlayHelper.requestOverlayPermission(act)
                                    }
                                } else {
                                    onToggleOverlayService()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00F0FF)
                            )
                        )
                    }

                    if (!hasOverlayPermission) {
                        Button(
                            onClick = {
                                (context as? android.app.Activity)?.let { act ->
                                    overlayHelper.requestOverlayPermission(act)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant 'Display Over Other Apps' Permission", fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    // Auto-start on boot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Start on Device Boot", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color.White)
                            Text("Automatically start MAYA background listener when phone powers on", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        }
                        Switch(
                            checked = isAutoStartEnabled,
                            onCheckedChange = { onToggleAutoStart() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00F0FF)
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    // Battery Optimization
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Battery Optimization", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color.White)
                            Text(
                                text = if (hasBatteryExemption) "Exempt (Background service will not be killed)" else "Restricted (Recommended to exempt)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasBatteryExemption) Color(0xFF10B981) else Color(0xFFFACC15)
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                overlayHelper.requestBatteryOptimization(context)
                                hasBatteryExemption = overlayHelper.hasBatteryOptimizationPermission()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00F0FF))
                        ) {
                            Text("Configure", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Voice & Speech Engine Category
        item {
            Text(
                text = "Continuous Voice Control & STT Engine",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF00F0FF),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Continuous Voice Mode Master Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Continuous Voice Listening", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color.White)
                            Text(
                                text = if (isContinuousMode) "Hands-free continuous recognition is ACTIVE" else "Push-to-talk mode active (Tap orb to speak)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isContinuousMode) Color(0xFF10B981) else Color(0xFF94A3B8)
                            )
                        }
                        Switch(
                            checked = isContinuousMode,
                            onCheckedChange = onContinuousModeToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00F0FF)
                            )
                        )
                    }

                    if (isContinuousMode) {
                        HorizontalDivider(color = Color(0xFF1E293B))

                        // Voice Trigger Mode Selector
                        Text("Voice Trigger Mode", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF00F0FF))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ContinuousVoiceMode.values().filter { it != ContinuousVoiceMode.PUSH_TO_TALK }.forEach { mode ->
                                val selected = continuousVoiceMode == mode
                                FilterChip(
                                    selected = selected,
                                    onClick = { onContinuousVoiceModeChange(mode) },
                                    label = { Text(mode.displayName, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00F0FF).copy(alpha = 0.2f),
                                        selectedLabelColor = Color(0xFF00F0FF),
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = Color(0xFF94A3B8)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Wake Word selection if in WAKE_WORD mode
                        if (continuousVoiceMode == ContinuousVoiceMode.WAKE_WORD) {
                            Text("Hotword Trigger", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF00F0FF))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WakeWordOption.values().forEach { word ->
                                    val selected = wakeWordOption == word
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onWakeWordChange(word) },
                                        label = { Text(word.displayName, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8).copy(alpha = 0.25f),
                                            selectedLabelColor = Color(0xFF38BDF8),
                                            containerColor = Color(0xFF1E293B),
                                            labelColor = Color(0xFF94A3B8)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1E293B))

                        // On-Device / Offline STT Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Prefer On-Device STT", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color.White)
                                Text(
                                    text = if (isDeviceOfflineAvailable) "Local engine supported (Zero latency & private)" else "Falls back to Google Cloud Speech",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDeviceOfflineAvailable) Color(0xFF10B981) else Color(0xFF94A3B8)
                                )
                            }
                            Switch(
                                checked = preferOfflineSpeech,
                                onCheckedChange = onPreferOfflineSpeechChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF00F0FF)
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    // Speech Speed Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("TTS Response Speech Rate", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            Text(String.format("%.1fx", ttsSpeed), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF00F0FF))
                        }
                        Slider(
                            value = ttsSpeed,
                            onValueChange = onTtsSpeedChange,
                            valueRange = 0.6f..1.8f,
                            steps = 5,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00F0FF),
                                activeTrackColor = Color(0xFF00F0FF),
                                inactiveTrackColor = Color(0xFF334155)
                            )
                        )
                    }
                }
            }
        }

        // Orb Theme Aesthetics Category
        item {
            Text(
                text = "Holographic Orb Themes",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF00F0FF),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (theme in OrbTheme.values()) {
                        val isSelected = currentOrbTheme == theme
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) theme.coreColor.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { onSelectOrbTheme(theme) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(theme.coreColor)
                                    )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(theme.title, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) Color.White else Color(0xFFCBD5E1))
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = theme.coreColor, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // User Account & API Keys
        item {
            Text(
                text = "Account & Security Credentials",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF00F0FF),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onOpenProfile,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("User Profile & Sessions", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onOpenApiKeys,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manage Encrypted API Keys (AES-GCM)", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // About / System Info
        item {
            Text(
                text = "System Architecture",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF00F0FF),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("MAYA Voice Engine: Version 1.0 Production", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color.White)
                    Text("Target Runtime: ${VersionUtils.osCodename} (API ${VersionUtils.sdkInt})", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                    Text("AI Brain: Gemini 3.5 Flash via Serverless REST Logic", style = MaterialTheme.typography.bodySmall, color = Color(0xFF38BDF8))
                    Text("Background Service: Foreground Service with Persistent Notification", style = MaterialTheme.typography.bodySmall, color = Color(0xFF10B981))
                    Text("System Overlay: Floating Draggable 3D Orb & Action HUD", style = MaterialTheme.typography.bodySmall, color = Color(0xFF00F0FF))
                    Text("Device Compatibility: Android 8.0 (API 26) through Android 16 (API 36)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))

                    HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "🌟 MADE BY MANI",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                        color = Color(0xFF00F0FF)
                    )
                }
            }
        }
    }
}

