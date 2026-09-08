package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.core.AutomationTask
import com.example.assistant.core.TaskEngine
import com.example.assistant.core.TaskLogEntry
import com.example.assistant.core.TaskState
import com.example.assistant.services.MayaAccessibilityService

/**
 * Developer & Live Task Automation Debug Panel for MAYA.
 * Shows:
 * - Current foreground package & activity
 * - Active Automation Task card with step-by-step progress
 * - Expected State vs Actual State
 * - Execution controls: [Pause/Resume] [Cancel/Stop] [Retry]
 * - Real-time chronological execution timeline
 * - Built-in verification test suite
 */
@Composable
fun TaskExecutionDebugPanel(
    taskEngine: TaskEngine,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTask by taskEngine.activeTask.collectAsState()
    val timelineLogs by taskEngine.timelineLogs.collectAsState()
    val stats by taskEngine.stats.collectAsState()
    val screenState by taskEngine.screenVisionState.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Live Task, 1: Timeline, 2: Screen Vision, 3: Test Suite

    val accessibilityService = MayaAccessibilityService.instance
    val fgPackage = accessibilityService?.currentPackage ?: "None / Applet"
    val fgActivity = accessibilityService?.currentActivity ?: "MainActivity"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A).copy(alpha = 0.95f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (activeTask != null) Color(0xFF00F0FF).copy(alpha = 0.6f) else Color(0xFF334155)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    activeTask?.state == TaskState.EXECUTING -> Color(0xFF10B981)
                                    activeTask?.state == TaskState.WAITING -> Color(0xFFF59E0B)
                                    activeTask?.state == TaskState.FAILED -> Color(0xFFEF4444)
                                    else -> Color(0xFF00F0FF)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "TASK AUTOMATION ENGINE",
                        color = Color(0xFF00F0FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activeTask != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF00F0FF).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "RUNNING: ${activeTask?.stepSummary}",
                                color = Color(0xFF00F0FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand/Collapse",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            // Quick summary when collapsed
            if (!isExpanded && activeTask != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Current: ${activeTask?.title}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${activeTask?.currentStep?.title} • Expected: ${activeTask?.currentStep?.expectedState}",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            // Expanded content
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    // System Foreground Telemetry
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Foreground Package:",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = fgPackage,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Active Activity:",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = fgActivity.substringAfterLast('.'),
                                    color = Color(0xFFA5B4FC),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Screen Role:",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = screenState.currentScreen,
                                    color = Color(0xFF34D399),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (screenState.hasDialog) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Interruption:",
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "⚠️ ${screenState.dialogTitle ?: "Dialog"}",
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Navigation Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF00F0FF),
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Task", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Timeline (${timelineLogs.size})", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Vision (${screenState.visibleElements.size})", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("Test Suite", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when (selectedTab) {
                        0 -> ActiveTaskTab(activeTask = activeTask, taskEngine = taskEngine)
                        1 -> TimelineTab(logs = timelineLogs)
                        2 -> ScreenVisionTab(screenState = screenState)
                        3 -> TestSuiteTab(onExecuteCommand = onExecuteCommand)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveTaskTab(activeTask: AutomationTask?, taskEngine: TaskEngine) {
    if (activeTask == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No Active Automation Task",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
                Text(
                    text = "Say \"Open YouTube and search Arijit Singh\" to test",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }
        }
        return
    }

    Column {
        // Task Title & State
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeTask.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID: ${activeTask.id} • Original: \"${activeTask.originalCommand}\"",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when (activeTask.state) {
                    TaskState.EXECUTING -> Color(0xFF10B981).copy(alpha = 0.2f)
                    TaskState.WAITING -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                    TaskState.FAILED -> Color(0xFFEF4444).copy(alpha = 0.2f)
                    else -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                }
            ) {
                Text(
                    text = activeTask.state.name,
                    color = when (activeTask.state) {
                        TaskState.EXECUTING -> Color(0xFF10B981)
                        TaskState.WAITING -> Color(0xFFF59E0B)
                        TaskState.FAILED -> Color(0xFFEF4444)
                        else -> Color(0xFF38BDF8)
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Linear Progress Indicator
        LinearProgressIndicator(
            progress = { activeTask.progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF00F0FF),
            trackColor = Color(0xFF334155),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Steps List
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            activeTask.steps.forEachIndexed { index, step ->
                val isCurrent = index == activeTask.currentStepIndex
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCurrent) Color(0xFF1E293B) else Color(0xFF0F172A),
                    border = if (isCurrent) BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.5f)) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Step number badge
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when (step.state) {
                                        TaskState.COMPLETED -> Color(0xFF10B981)
                                        TaskState.EXECUTING -> Color(0xFF00F0FF)
                                        TaskState.FAILED -> Color(0xFFEF4444)
                                        else -> Color(0xFF334155)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = step.title,
                                color = if (isCurrent) Color.White else Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(
                                text = "Expected: ${step.expectedState}" +
                                        if (step.attemptCount > 1) " (Attempt ${step.attemptCount})" else "",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                        }

                        // Step Status Indicator
                        Text(
                            text = step.state.name,
                            color = when (step.state) {
                                TaskState.COMPLETED -> Color(0xFF10B981)
                                TaskState.EXECUTING -> Color(0xFF00F0FF)
                                TaskState.WAITING -> Color(0xFFF59E0B)
                                TaskState.FAILED -> Color(0xFFEF4444)
                                else -> Color(0xFF64748B)
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Task Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (activeTask.isPaused) taskEngine.resumeTask() else taskEngine.pauseTask()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (activeTask.isPaused) "Resume" else "Pause", fontSize = 12.sp)
            }

            Button(
                onClick = { taskEngine.retryCurrentStep() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Retry Step", fontSize = 12.sp)
            }

            Button(
                onClick = { taskEngine.cancelCurrentTask("Cancelled by user") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel Task", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TimelineTab(logs: List<TaskLogEntry>) {
    if (logs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No timeline events logged yet.", color = Color(0xFF64748B), fontSize = 12.sp)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(logs) { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = entry.timeFormatted,
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(60.dp)
                    )
                    Text(
                        text = entry.message,
                        color = when (entry.level) {
                            "ERROR" -> Color(0xFFEF4444)
                            "WARN" -> Color(0xFFF59E0B)
                            "SUCCESS" -> Color(0xFF10B981)
                            else -> Color(0xFFCBD5E1)
                        },
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TestSuiteTab(onExecuteCommand: (String) -> Unit) {
    val testScenarios = listOf(
        "Open YouTube and search Arijit Singh" to "Compound YouTube search test",
        "Open YouTube, search Arijit Singh, play the first song" to "Full 3-step YouTube playback test",
        "YouTube kholo aur Arijit Singh search karo" to "Hindi/Hinglish YouTube search test",
        "Ab Arijit Singh search karo" to "Contextual follow-up search",
        "Ab first video play karo" to "Contextual follow-up playback",
        "Open Spotify and play Kesariya" to "Spotify music search & play",
        "Open WhatsApp, find Rahul, and send hello" to "WhatsApp multi-step messaging",
        "Open Chrome, search Google, and open the first result" to "Web search automation",
        "Open Contacts, find Mom, and call her" to "Phone call resolution",
        "Open Settings and go to Wi-Fi" to "Settings sub-page automation"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Tap any verified test scenario to run:",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp
        )

        testScenarios.forEach { (cmd, desc) ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExecuteCommand(cmd) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "\"$cmd\"",
                            color = Color(0xFF00F0FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = desc,
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenVisionTab(screenState: com.example.assistant.core.ScreenStateModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Screen overview header
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "SEMANTIC SCREEN INSPECTION",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Package: ${screenState.currentPackage.ifEmpty { "None" }}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Screen: ${screenState.currentScreen} • Targets: ${screenState.visibleElements.size}",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                if (!screenState.dialogTitle.isNullOrBlank()) {
                    Text(
                        text = "Dialog/Interruption: ${screenState.dialogTitle}",
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (screenState.visibleElements.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No visual targets currently mapped.\nEnsure Accessibility Service is active.",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(screenState.visibleElements) { target ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (target.role) {
                                        com.example.assistant.core.VisualRole.SEARCH_INPUT,
                                        com.example.assistant.core.VisualRole.SEARCH_BUTTON -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                                        com.example.assistant.core.VisualRole.PLAY_BUTTON -> Color(0xFF10B981).copy(alpha = 0.2f)
                                        com.example.assistant.core.VisualRole.DIALOG_DISMISS -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                        else -> Color(0xFF64748B).copy(alpha = 0.2f)
                                    }
                                ) {
                                    Text(
                                        text = target.role.name,
                                        color = when (target.role) {
                                            com.example.assistant.core.VisualRole.SEARCH_INPUT,
                                            com.example.assistant.core.VisualRole.SEARCH_BUTTON -> Color(0xFF38BDF8)
                                            com.example.assistant.core.VisualRole.PLAY_BUTTON -> Color(0xFF10B981)
                                            com.example.assistant.core.VisualRole.DIALOG_DISMISS -> Color(0xFFF59E0B)
                                            else -> Color(0xFF94A3B8)
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = "Center: (${target.centerX.toInt()}, ${target.centerY.toInt()})",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (target.text.isNotBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Text: \"${target.text}\"",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                            if (target.contentDescription.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Desc: \"${target.contentDescription}\"",
                                    color = Color(0xFFA5B4FC),
                                    fontSize = 10.sp
                                )
                            }
                            if (target.viewId.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Id: ${target.viewId.substringAfterLast('/')}",
                                    color = Color(0xFF64748B),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
