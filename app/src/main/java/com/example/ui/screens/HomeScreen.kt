package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.assistant.actions.ScreenMapper
import com.example.assistant.actions.ScreenOverlay
import com.example.assistant.core.AssistantState
import com.example.assistant.core.OrbTheme
import com.example.ui.components.MayaOrb
import com.example.ui.components.QuickActions
import com.example.ui.components.VoiceWaveform

@Composable
fun HomeScreen(
    state: AssistantState,
    orbTheme: OrbTheme,
    audioLevel: Float,
    transcript: String,
    response: String,
    screenMap: ScreenMapper.ScreenMap? = null,
    visualFeedback: ScreenOverlay.VisualActionFeedback? = null,
    isContinuousMode: Boolean,
    onToggleListen: () -> Unit,
    onToggleContinuous: () -> Unit,
    onMapScreen: () -> Unit,
    onExecuteCommand: (String) -> Unit,
    onStopSpeech: () -> Unit,
    onOpenSettings: () -> Unit,
    orbCustomization: com.example.assistant.ultimate.OrbCustomization? = null,
    user: com.example.data.local.entities.UserEntity? = null,
    creatorCredit: String = "🌟 MADE BY MANI",
    dailyMessage: String = "",
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var textInputQuery by remember { mutableStateOf("") }
    var showTextInput by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070A13),
                        Color(0xFF0D1322),
                        Color(0xFF070A13)
                    )
                )
            )
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFF00F0FF).copy(alpha = 0.8f), CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_orb_core),
                        contentDescription = "MAYA Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MAYA",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = Color(0xFF00F0FF)
                )
                Spacer(modifier = Modifier.width(10.dp))
                StatusPill(status = state)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Screen Mapper Radar Button
                IconButton(
                    onClick = onMapScreen,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00F0FF).copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = "Map Screen",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onToggleContinuous,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isContinuousMode) Color(0xFF10B981).copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.AllInclusive,
                        contentDescription = "Continuous Mode",
                        tint = if (isContinuousMode) Color(0xFF10B981) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { showTextInput = !showTextInput },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Keyboard Input",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Settings",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onOpenProfile,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00F0FF).copy(alpha = 0.2f))
                ) {
                    Text(
                        text = (user?.displayName?.take(1) ?: "U").uppercase(),
                        color = Color(0xFF00F0FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // User & Creator Credit Sub-Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.7f))
                .clickable { onOpenProfile() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user?.displayName ?: "Guest User",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Badge(containerColor = if (user?.isPremium == true) Color(0xFF10B981) else Color(0xFF00F0FF).copy(alpha = 0.3f)) {
                    Text(if (user?.isPremium == true) "PRO" else "GUEST", color = Color.White, fontSize = 9.sp)
                }
            }
            Text(
                text = creatorCredit,
                color = Color(0xFF00F0FF),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        // Center Content (Orb + Response Area)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Interactive 3D Glowing Energy Orb
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                MayaOrb(
                    state = state,
                    orbTheme = orbTheme,
                    audioLevel = audioLevel,
                    customization = orbCustomization,
                    onClick = onToggleListen,
                    modifier = Modifier.size(240.dp)
                )
            }

            // Continuous Mode Status Indicator
            if (isContinuousMode) {
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "CONTINUOUS VOICE ACTIVE",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Real-time sinusoidal Voice Waveform
            VoiceWaveform(
                isActive = state == AssistantState.LISTENING || state == AssistantState.SPEAKING,
                audioLevel = audioLevel,
                primaryColor = state.primaryColor,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            // Visual Action & Feedback Banner (if active)
            if (visualFeedback != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A).copy(alpha = 0.9f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF00F0FF).copy(alpha = 0.6f), Color(0xFF38BDF8).copy(alpha = 0.3f))
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (visualFeedback.type) {
                                "TAP", "TAP_LABEL" -> Icons.Default.TouchApp
                                "HIGHLIGHT", "SEARCH_RESULTS" -> Icons.Default.Search
                                "SCREEN_MAP" -> Icons.Default.Radar
                                "ACTION" -> Icons.Default.PlayArrow
                                else -> Icons.Default.RecordVoiceOver
                            },
                            contentDescription = null,
                            tint = Color(0xFF00F0FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "VISION & ACTION FEEDBACK",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = Color(0xFF38BDF8)
                            )
                            Text(
                                text = visualFeedback.text.ifBlank { "Action executed at (${visualFeedback.x.toInt()}, ${visualFeedback.y.toInt()})" },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Transcript / Response Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (transcript.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = transcript,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }

                if (response.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F172A)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF00F0FF).copy(alpha = 0.4f), Color(0xFF6366F1).copy(alpha = 0.4f))
                            )
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(state.primaryColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "MAYA",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF00F0FF)
                                    )
                                }
                                if (state == AssistantState.SPEAKING) {
                                    IconButton(
                                        onClick = onStopSpeech,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                            contentDescription = "Stop speaking",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = response,
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                                color = Color.White
                            )
                        }
                    }
                } else if (transcript.isBlank()) {
                    Text(
                        text = when (state) {
                            AssistantState.LISTENING -> "Listening to your voice... Speak now."
                            AssistantState.THINKING -> "Processing command..."
                            AssistantState.EXECUTING -> "Executing request..."
                            AssistantState.SPEAKING -> "Speaking response..."
                            else -> "Tap the Orb or say \"Hey Maya\" to speak"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // Active Screen Map Snapshot Card
                if (screenMap != null && screenMap.elements.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E293B).copy(alpha = 0.45f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📱 ${screenMap.packageName.takeLast(25)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "${screenMap.elements.size} items • ${screenMap.clickableElements.size} taps",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                }
            }

            // Quick Actions Chips
            QuickActions(
                onActionClick = { cmd ->
                    onExecuteCommand(cmd)
                },
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Silent Keyboard Query Field (collapsible)
        AnimatedVisibility(visible = showTextInput) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInputQuery,
                    onValueChange = { textInputQuery = it },
                    placeholder = { Text("Type command (e.g. Find search on screen)...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00F0FF),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (textInputQuery.isNotBlank()) {
                            onExecuteCommand(textInputQuery)
                            textInputQuery = ""
                            showTextInput = false
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF00F0FF))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send command",
                        tint = Color(0xFF070A13)
                    )
                }
            }
        }

        // Bottom Action Bar with Glowing Mic Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = onToggleListen,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (state == AssistantState.LISTENING) Color(0xFF10B981) else Color(0xFF00F0FF)
                )
            ) {
                Icon(
                    imageVector = if (state == AssistantState.LISTENING) Icons.Default.Mic else Icons.Default.MicNone,
                    contentDescription = "Voice Input",
                    tint = Color(0xFF070A13),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun StatusPill(status: AssistantState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(status.primaryColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(status.primaryColor)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = status.primaryColor
        )
    }
}
