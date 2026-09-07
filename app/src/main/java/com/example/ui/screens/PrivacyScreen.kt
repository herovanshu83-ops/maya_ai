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
import androidx.core.content.ContextCompat

@Composable
fun PrivacyScreen(
    onRequestAllPermissions: () -> Unit,
    onWipeAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showWipeConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Privacy & Permissions Center",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "MAYA operates on a Local-First architecture. Audio processing and memory facts stay entirely in your secure local device storage.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        }

        item {
            Button(
                onClick = onRequestAllPermissions,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF))
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF070A13))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant / Verify All Permissions", color = Color(0xFF070A13), fontWeight = FontWeight.Bold)
            }
        }

        item {
            Text(
                text = "Permission Status",
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
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PermissionStatusRow(
                        title = "Microphone & Voice Input",
                        desc = "Required for speech recognition and wake phrase",
                        isGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED,
                        icon = Icons.Default.Mic
                    )
                    HorizontalDivider(color = Color(0xFF1E293B))
                    PermissionStatusRow(
                        title = "Camera & Flashlight",
                        desc = "Required for flashlight toggle and camera launch",
                        isGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED,
                        icon = Icons.Default.CameraAlt
                    )
                    HorizontalDivider(color = Color(0xFF1E293B))
                    PermissionStatusRow(
                        title = "Phone & Contacts",
                        desc = "Required to make phone calls and compose SMS",
                        isGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED,
                        icon = Icons.Default.Contacts
                    )
                    HorizontalDivider(color = Color(0xFF1E293B))
                    PermissionStatusRow(
                        title = "Location & GPS",
                        desc = "Required for navigation and nearby place queries",
                        isGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED,
                        icon = Icons.Default.LocationOn
                    )
                    HorizontalDivider(color = Color(0xFF1E293B))
                    val isAccessibilityActive = com.example.assistant.services.MayaAccessibilityService.isServiceActive(context)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Accessibility,
                                contentDescription = null,
                                tint = if (isAccessibilityActive) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Accessibility Automation Service",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = Color.White
                                )
                                Text(
                                    text = "Automates in-app searches, taps, Wi-Fi & alarm controls",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Text(
                                text = if (isAccessibilityActive) "Active" else "Enable",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isAccessibilityActive) Color(0xFF10B981) else Color(0xFF00F0FF)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            OutlinedButton(
                onClick = { showWipeConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFEF4444))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Erase All Memories & Activity Log", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            title = { Text("Erase All Data?") },
            text = { Text("This will permanently delete your entire command activity log and all stored personal memories from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        onWipeAllData()
                        showWipeConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PermissionStatusRow(
    title: String,
    desc: String,
    isGranted: Boolean,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isGranted) Color(0xFF10B981) else Color(0xFFEF4444))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color.White)
                Text(text = desc, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
            }
        }
        Text(
            text = if (isGranted) "Granted" else "Pending",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isGranted) Color(0xFF10B981) else Color(0xFFF59E0B)
        )
    }
}
