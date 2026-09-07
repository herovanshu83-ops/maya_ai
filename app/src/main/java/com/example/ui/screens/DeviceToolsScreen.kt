package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.DeviceUtils
import com.example.utils.VersionUtils

@Composable
fun DeviceToolsScreen(
    onExecuteVoiceCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var batteryInfo by remember { mutableStateOf(DeviceUtils.getBatteryInfo(context)) }
    var memoryInfo by remember { mutableStateOf(DeviceUtils.getMemoryInfo(context)) }
    var storageInfo by remember { mutableStateOf(DeviceUtils.getStorageInfo()) }
    var isFlashlightOn by remember { mutableStateOf(DeviceUtils.isFlashlightOn()) }
    var volumeLevel by remember { mutableFloatStateOf(0.7f) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Device Telemetry & Controls",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Real-time hardware status and voice quick toggles",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        }

        // Telemetry Grid Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TelemetryCard(
                    title = "Battery",
                    value = "${batteryInfo.percentage}%",
                    subtitle = batteryInfo.statusText,
                    icon = Icons.Default.BatteryChargingFull,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                TelemetryCard(
                    title = "RAM Free",
                    value = "${memoryInfo.freeRamMb} MB",
                    subtitle = "${memoryInfo.percentUsed}% in use",
                    icon = Icons.Default.Memory,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TelemetryCard(
                    title = "Storage Free",
                    value = "${storageInfo.freeGb} GB",
                    subtitle = "${storageInfo.totalGb} GB total",
                    icon = Icons.Default.Storage,
                    tint = Color(0xFFA855F7),
                    modifier = Modifier.weight(1f)
                )
                TelemetryCard(
                    title = "OS Platform",
                    value = VersionUtils.osCodename.substringBefore(" ("),
                    subtitle = "API ${VersionUtils.sdkInt}",
                    icon = Icons.Default.Android,
                    tint = Color(0xFF00F0FF),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Controls
        item {
            Text(
                text = "Quick Hardware Controls",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF00F0FF),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Flashlight Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FlashlightOn, contentDescription = null, tint = Color(0xFFF59E0B))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Torch Flashlight", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                Text("Toggle LED light", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                        Switch(
                            checked = isFlashlightOn,
                            onCheckedChange = {
                                val state = DeviceUtils.toggleFlashlight(context, it)
                                isFlashlightOn = state
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00F0FF)
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    // Volume slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Media Volume", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            Text("${(volumeLevel * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF00F0FF))
                        }
                        Slider(
                            value = volumeLevel,
                            onValueChange = {
                                volumeLevel = it
                                DeviceUtils.setVolume(context, (it * 100).toInt())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00F0FF),
                                activeTrackColor = Color(0xFF00F0FF),
                                inactiveTrackColor = Color(0xFF334155)
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    // System Settings Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { DeviceUtils.openWifiSettings(context) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wi-Fi", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { DeviceUtils.openBluetoothSettings(context) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bluetooth", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { DeviceUtils.openDisplaySettings(context) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Brightness6, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Display", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = tint.copy(alpha = 0.8f))
        }
    }
}
