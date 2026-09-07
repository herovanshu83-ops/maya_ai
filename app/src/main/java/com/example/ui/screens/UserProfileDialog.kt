package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.user.UserManager
import com.example.data.local.entities.UserEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserProfileDialog(
    user: UserEntity?,
    isGuest: Boolean,
    userManager: UserManager,
    onOpenApiKeys: () -> Unit,
    onOpenAuth: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "User Profile",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // User Avatar and info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00F0FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user?.displayName?.take(1) ?: "M").uppercase(),
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = user?.displayName ?: if (isGuest) "Guest User" else "Offline Account",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = user?.email ?: "guest@maya.local",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Badge(containerColor = if (user?.isPremium == true) Color(0xFF10B981) else Color(0xFF6366F1)) {
                            Text(
                                text = if (user?.isPremium == true) "PRO USER" else if (isGuest) "GUEST" else "STANDARD",
                                color = Color.White,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B))

                // Actions List
                OutlinedButton(
                    onClick = onOpenApiKeys,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Encrypted API Keys", color = Color.White, fontSize = 12.sp)
                }

                if (isGuest || user == null) {
                    Button(
                        onClick = onOpenAuth,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign In or Register Account", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                userManager.logout()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }

                    TextButton(
                        onClick = {
                            scope.launch {
                                userManager.deleteAccount()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Permanently Delete Account & Data", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B))

                // Creator Credit
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🌟 MADE BY MANI",
                        color = Color(0xFF00F0FF),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Architect & Lead Creator",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }
        },
        confirmButton = {}
    )
}
