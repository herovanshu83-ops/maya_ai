package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.actions.ScreenMapper
import com.example.assistant.core.AssistantState
import com.example.assistant.core.AutomationTask
import com.example.assistant.core.OrbTheme
import com.example.assistant.core.TaskState
import com.example.assistant.services.MayaAccessibilityService
import com.example.ui.components.MayaOrb
import com.example.ui.theme.MayaAccent
import com.example.ui.theme.StateGreen
import com.example.ui.theme.StateRed

@Composable
fun HomeScreen(
    state: AssistantState,
    orbTheme: OrbTheme = OrbTheme.CYBER_CYAN,
    audioLevel: Float = 0f,
    transcript: String = "",
    response: String = "",
    activeTask: AutomationTask? = null,
    screenMap: ScreenMapper.ScreenMap? = null,
    onToggleListen: () -> Unit,
    onStopTask: () -> Unit = {},
    onInspectScreen: () -> Unit = {},
    onOpenTasks: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAccessibilityActive = MayaAccessibilityService.isServiceActive(context)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_mic")
    val micPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == AssistantState.LISTENING) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ==========================================
        // TOP APP BAR: MAYA + Subtitle + Settings ⚙
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MAYA",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Your AI Assistant",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ==========================================
        // SCREEN STATUS INDICATOR (Subtle Pill)
        // ==========================================
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isAccessibilityActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            },
            border = BorderStroke(
                1.dp,
                if (isAccessibilityActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .clickable { onInspectScreen() }
                .padding(bottom = 8.dp)
                .testTag("screen_status_indicator")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isAccessibilityActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = if (isAccessibilityActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isAccessibilityActive) "Screen Understanding Active" else "Screen Understanding Inactive",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isAccessibilityActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ==========================================
        // SCROLLABLE CENTER REGION (Orb, States, Tasks)
        // ==========================================
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Center Subtle Animated Orb
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                MayaOrb(
                    state = state,
                    orbTheme = orbTheme,
                    audioLevel = audioLevel,
                    onClick = onToggleListen,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // State Label (Idle, Listening, Thinking, Executing, Completed, Error)
            AssistantStateLabel(state = state)

            // Spoken Response or Live Transcript (Subtle text bubble)
            AnimatedVisibility(
                visible = transcript.isNotBlank() || response.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (transcript.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = transcript,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (transcript.isNotBlank() && response.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (response.isNotBlank()) {
                            Text(
                                text = response,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // ==========================================
            // CURRENT TASK CARD (Only shown when active)
            // ==========================================
            AnimatedVisibility(
                visible = activeTask != null && (
                    activeTask.state == TaskState.EXECUTING ||
                    activeTask.state == TaskState.PLANNING ||
                    activeTask.state == TaskState.WAITING ||
                    activeTask.state == TaskState.VERIFYING ||
                    activeTask.state == TaskState.RETRYING
                ),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (activeTask != null) {
                    CurrentTaskCard(
                        task = activeTask,
                        onStop = onStopTask
                    )
                }
            }

            // ==========================================
            // QUICK ACTIONS (Minimal 2-shortcut layout)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Screen Inspection Shortcut
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(76.dp)
                        .clickable { onInspectScreen() }
                        .testTag("quick_action_screen")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Screen",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Screen",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Tasks & History Shortcut
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(76.dp)
                        .clickable { onOpenTasks() }
                        .testTag("quick_action_tasks")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = "Tasks",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tasks",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // ==========================================
        // VOICE BUTTON (Bottom-Center, Large Circular)
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, top = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                // Outer subtle pulse ring when listening
                if (state == AssistantState.LISTENING) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(micPulseScale)
                            .clip(CircleShape)
                            .background(StateGreen.copy(alpha = 0.2f))
                    )
                }

                FilledIconButton(
                    onClick = onToggleListen,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .testTag("voice_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = when (state) {
                            AssistantState.LISTENING -> StateGreen
                            AssistantState.THINKING -> MayaAccent
                            AssistantState.EXECUTING -> MayaAccent
                            else -> MaterialTheme.colorScheme.primary
                        },
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (state == AssistantState.LISTENING) Icons.Default.Mic else Icons.Default.MicNone,
                        contentDescription = if (state == AssistantState.LISTENING) "Listening" else "Tap to Speak",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (state == AssistantState.LISTENING) "Listening..." else "Tap to Speak",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = if (state == AssistantState.LISTENING) StateGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * State label presenting the assistant's clear status with clean icons.
 */
@Composable
private fun AssistantStateLabel(state: AssistantState) {
    val (label, color) = when (state) {
        AssistantState.IDLE -> "Ready" to MaterialTheme.colorScheme.onSurfaceVariant
        AssistantState.LISTENING -> "🎙 Listening..." to StateGreen
        AssistantState.HEARING -> "👂 Hearing..." to MayaAccent
        AssistantState.THINKING -> "🧠 Thinking..." to MayaAccent
        AssistantState.EXECUTING -> "⚡ Executing..." to MayaAccent
        AssistantState.SPEAKING -> "🗣 Speaking..." to MayaAccent
        AssistantState.ERROR -> "⚠️ Error" to StateRed
        AssistantState.OFFLINE -> "○ Offline" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        ),
        color = color,
        textAlign = TextAlign.Center
    )
}

/**
 * Minimal Current Task Card displaying target app, step progress, and stop button.
 */
@Composable
private fun CurrentTaskCard(
    task: AutomationTask,
    onStop: () -> Unit
) {
    val currentStepNum = if (task.currentStepIndex >= 0) task.currentStepIndex + 1 else 1
    val totalSteps = task.steps.size.coerceAtLeast(1)
    val appPkg = task.targetPackage ?: ""
    val appName = if (appPkg.isNotBlank()) appPkg.substringAfterLast('.').replaceFirstChar { it.uppercase() } else ""

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .testTag("current_task_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Task",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = task.state.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (appName.isNotBlank() && !appName.contains("example")) {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Step Progress (e.g. ●●●○ 3/4)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalSteps) { idx ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx < currentStepNum) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                        )
                    }
                }

                Text(
                    text = "$currentStepNum / $totalSteps",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action stop button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onStop,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, StateRed.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StateRed),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
