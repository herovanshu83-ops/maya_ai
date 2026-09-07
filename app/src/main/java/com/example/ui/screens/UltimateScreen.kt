package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.assistant.ultimate.*

enum class UltimateSection(val title: String, val icon: ImageVector) {
    PERSONALITY("Personality", Icons.Default.Psychology),
    SMART_HOME("Smart Home", Icons.Default.Home),
    VEHICLE("Tesla / Auto", Icons.Default.DirectionsCar),
    HEALTH("Health", Icons.Default.Favorite),
    GAMING("Booster", Icons.Default.SportsEsports),
    ORB_STUDIO("Orb Studio", Icons.Default.AutoAwesome),
    SECURITY("Privacy & Offline", Icons.Default.Shield)
}

@Composable
fun UltimateScreen(
    ultimateManager: UltimateManager,
    onExecuteVoiceCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf(UltimateSection.PERSONALITY) }

    val personality by ultimateManager.personality.collectAsStateWithLifecycle()
    val mood by ultimateManager.userMood.collectAsStateWithLifecycle()
    val devices by ultimateManager.devices.collectAsStateWithLifecycle()
    val vehicle by ultimateManager.vehicle.collectAsStateWithLifecycle()
    val health by ultimateManager.health.collectAsStateWithLifecycle()
    val gaming by ultimateManager.gaming.collectAsStateWithLifecycle()
    val orbCustomization by ultimateManager.orbCustomization.collectAsStateWithLifecycle()
    val offlineModels by ultimateManager.offlineModels.collectAsStateWithLifecycle()
    val plugins by ultimateManager.plugins.collectAsStateWithLifecycle()
    val isVoiceAuthEnabled by ultimateManager.isVoiceAuthEnabled.collectAsStateWithLifecycle()
    val isPrivacyMode by ultimateManager.isPrivacyMode.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "MAYA ULTIMATE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF00F0FF)
                    )
                )
                Text(
                    text = "Autonomous Intelligence & Next-Gen Modules",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8)
                    )
                )
            }
            AssistChip(
                onClick = { onExecuteVoiceCommand("how are you feeling today?") },
                label = { Text("Ask Mood", color = Color(0xFF00F0FF), fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.SentimentSatisfied, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(16.dp)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFF0F172A)
                ),
                border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.4f))
            )
        }

        // Section Navigation Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(UltimateSection.values()) { section ->
                val isSelected = selectedSection == section
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedSection = section },
                    label = { Text(section.title, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = section.title,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00F0FF).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF00F0FF),
                        selectedLeadingIconColor = Color(0xFF00F0FF),
                        containerColor = Color(0xFF0F172A),
                        labelColor = Color(0xFF94A3B8),
                        iconColor = Color(0xFF94A3B8)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) Color(0xFF00F0FF) else Color(0xFF1E293B)
                    )
                )
            }
        }

        // Section Body
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            when (selectedSection) {
                UltimateSection.PERSONALITY -> {
                    item {
                        PersonalitySection(
                            currentPersonality = personality,
                            currentMood = mood,
                            onSelectPersonality = { ultimateManager.setPersonality(it) },
                            onSelectMood = { ultimateManager.setMood(it) },
                            onExecuteVoiceCommand = onExecuteVoiceCommand
                        )
                    }
                }
                UltimateSection.SMART_HOME -> {
                    item {
                        SmartHomeSection(
                            devices = devices,
                            onToggleDevice = { ultimateManager.toggleDevice(it) },
                            onToggleAllLights = { ultimateManager.setAllLights(it) },
                            onApplyScene = { ultimateManager.applyScene(it) },
                            onExecuteVoiceCommand = onExecuteVoiceCommand
                        )
                    }
                }
                UltimateSection.VEHICLE -> {
                    item {
                        VehicleSection(
                            vehicle = vehicle,
                            onToggleLock = { ultimateManager.toggleTeslaLock() },
                            onToggleClimate = { ultimateManager.toggleTeslaClimate() },
                            onToggleTrunk = { ultimateManager.toggleTeslaTrunk() },
                            onToggleFrunk = { ultimateManager.toggleTeslaFrunk() },
                            onExecuteVoiceCommand = onExecuteVoiceCommand
                        )
                    }
                }
                UltimateSection.HEALTH -> {
                    item {
                        HealthSection(
                            health = health,
                            onLogWater = { ultimateManager.logWater(0.25f) },
                            onExecuteVoiceCommand = onExecuteVoiceCommand
                        )
                    }
                }
                UltimateSection.GAMING -> {
                    item {
                        GamingSection(
                            gaming = gaming,
                            onToggleGaming = { ultimateManager.toggleGamingMode() },
                            onToggleRecord = { ultimateManager.toggleScreenRecording() },
                            onExecuteVoiceCommand = onExecuteVoiceCommand
                        )
                    }
                }
                UltimateSection.ORB_STUDIO -> {
                    item {
                        OrbStudioSection(
                            customization = orbCustomization,
                            onUpdateCustomization = { ultimateManager.updateOrbCustomization(it) }
                        )
                    }
                }
                UltimateSection.SECURITY -> {
                    item {
                        SecurityOfflineSection(
                            isVoiceAuth = isVoiceAuthEnabled,
                            isPrivacy = isPrivacyMode,
                            offlineModels = offlineModels,
                            plugins = plugins,
                            onToggleVoiceAuth = { ultimateManager.toggleVoiceAuth() },
                            onTogglePrivacy = { ultimateManager.togglePrivacyMode() },
                            onExecuteVoiceCommand = onExecuteVoiceCommand
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalitySection(
    currentPersonality: MayaPersonality,
    currentMood: MayaMood,
    onSelectPersonality: (MayaPersonality) -> Unit,
    onSelectMood: (MayaMood) -> Unit,
    onExecuteVoiceCommand: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFF00F0FF).copy(alpha = 0.5f), Color(0xFF7000FF).copy(alpha = 0.5f))))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "MAYA Personalities",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Switch MAYA's persona, tone, and spoken demeanor dynamically.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                )

                MayaPersonality.values().forEach { p ->
                    val isSelected = currentPersonality == p
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF00F0FF).copy(alpha = 0.15f) else Color(0xFF1E293B).copy(alpha = 0.5f))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF00F0FF) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectPersonality(p) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = p.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isSelected) Color(0xFF00F0FF) else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(containerColor = Color(0xFF00F0FF)) { Text("ACTIVE", fontSize = 9.sp, color = Color.Black) }
                                }
                            }
                            Text(
                                text = p.description,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                            )
                            Text(
                                text = "\"${p.sampleGreeting}\"",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF38BDF8), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 11.sp)
                            )
                        }
                        IconButton(onClick = { onExecuteVoiceCommand("make maya ${p.title.lowercase()}") }) {
                            Icon(Icons.Default.PlayCircle, contentDescription = "Preview", tint = Color(0xFF00F0FF))
                        }
                    }
                }
            }
        }

        // Mood AI
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Detected User Emotion",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Acoustic sentiment & speech cadence analysis",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                        )
                    }
                    Badge(containerColor = Color(0xFF10B981)) { Text(currentMood.title, color = Color.White, fontSize = 11.sp) }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MayaMood.values().forEach { m ->
                        val isSelected = currentMood == m
                        OutlinedButton(
                            onClick = { onSelectMood(m) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) Color(0xFF00F0FF).copy(alpha = 0.2f) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF00F0FF) else Color(0xFF334155)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(m.title, fontSize = 10.sp, color = if (isSelected) Color(0xFF00F0FF) else Color(0xFF94A3B8))
                        }
                    }
                }

                Text(
                    text = "MAYA Response Cadence: ${currentMood.responseStyle}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF38BDF8), fontSize = 12.sp)
                )
            }
        }
    }
}

@Composable
fun SmartHomeSection(
    devices: List<SmartDevice>,
    onToggleDevice: (String) -> Boolean,
    onToggleAllLights: (Boolean) -> Unit,
    onApplyScene: (String) -> Unit,
    onExecuteVoiceCommand: (String) -> Unit
) {
    val allLightsOn = devices.filter { it.type == DeviceType.LIGHT }.any { it.isOn }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Quick scenes
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Smart Automation Scenes",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onApplyScene("movie") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7000FF).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White)
                            Text("Movie", fontSize = 11.sp, color = Color.White)
                        }
                    }
                    Button(
                        onClick = { onApplyScene("study") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF).copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color(0xFF00F0FF))
                            Text("Study", fontSize = 11.sp, color = Color(0xFF00F0FF))
                        }
                    }
                    Button(
                        onClick = { onApplyScene("vacation") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981).copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = Color(0xFF10B981))
                            Text("Vacation", fontSize = 11.sp, color = Color(0xFF10B981))
                        }
                    }
                }
            }
        }

        // Lighting & Master Controls
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Connected Devices (${devices.size} online)",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("All Smart Lights", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(if (allLightsOn) "Active across Hue & LIFX" else "All lights powered off", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                    Switch(
                        checked = allLightsOn,
                        onCheckedChange = { onToggleAllLights(it) }
                    )
                }

                HorizontalDivider(color = Color(0xFF1E293B))

                devices.take(4).forEach { device ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(device.name, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("${device.room} • ${device.platform}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Switch(
                            checked = device.isOn,
                            onCheckedChange = { onToggleDevice(device.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleSection(
    vehicle: VehicleStatus,
    onToggleLock: () -> Boolean,
    onToggleClimate: () -> Boolean,
    onToggleTrunk: () -> Boolean,
    onToggleFrunk: () -> Boolean,
    onExecuteVoiceCommand: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFFE11D48), Color(0xFF7000FF))))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(vehicle.model, style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Black))
                    Text(vehicle.location, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)))
                }
                Badge(containerColor = if (vehicle.isLocked) Color(0xFF10B981) else Color(0xFFF59E0B)) {
                    Text(if (vehicle.isLocked) "LOCKED" else "UNLOCKED", color = Color.White, fontSize = 10.sp)
                }
            }

            // Battery Meter
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Battery Reserve", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("${vehicle.batteryPercent}% (${vehicle.estimatedRangeMiles} mi)", color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                LinearProgressIndicator(
                    progress = { vehicle.batteryPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = Color(0xFF00F0FF),
                    trackColor = Color(0xFF1E293B)
                )
            }

            // Quick Actions Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onToggleLock() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.4f))
                ) {
                    Icon(if (vehicle.isLocked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (vehicle.isLocked) "Unlock" else "Lock", fontSize = 11.sp, color = Color.White)
                }

                OutlinedButton(
                    onClick = { onToggleClimate() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.AcUnit, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (vehicle.climateOn) Color(0xFF00F0FF) else Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (vehicle.climateOn) "Climate On" else "Climate", fontSize = 11.sp, color = Color.White)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onToggleTrunk() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (vehicle.trunkOpen) "Close Trunk" else "Open Trunk", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
                OutlinedButton(
                    onClick = { onToggleFrunk() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (vehicle.frunkOpen) "Close Frunk" else "Open Frunk", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }

            Button(
                onClick = { onExecuteVoiceCommand("find ev charging station") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.EvStation, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Find Nearest EV Supercharger", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun HealthSection(
    health: HealthData,
    onLogWater: () -> Unit,
    onExecuteVoiceCommand: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Health & Fitness Co-Pilot", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                Badge(containerColor = Color(0xFF10B981)) { Text("Synced", color = Color.White, fontSize = 10.sp) }
            }

            // Steps Progress
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Daily Steps", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("${health.stepsToday} / ${health.stepGoal}", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                LinearProgressIndicator(
                    progress = { (health.stepsToday.toFloat() / health.stepGoal.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFF1E293B)
                )
            }

            // Stats grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Heart Rate", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("${health.heartRateBpm} BPM", color = Color(0xFFEF4444), fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Sleep Score", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("${health.sleepQualityScore}% (${health.sleepHours}h)", color = Color(0xFF818CF8), fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Water Today", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("${String.format(java.util.Locale.US, "%.2f", health.waterLiters)} L", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onLogWater,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF).copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.LocalDrink, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+250ml Water", color = Color(0xFF00F0FF), fontSize = 11.sp)
                }

                Button(
                    onClick = { onExecuteVoiceCommand("suggest a workout") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981).copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Workout AI", color = Color(0xFF10B981), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun GamingSection(
    gaming: GamingState,
    onToggleGaming: () -> Boolean,
    onToggleRecord: () -> Boolean,
    onExecuteVoiceCommand: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Game Turbo Booster", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    Text("Do Not Disturb, RAM purge & 120Hz refresh rate lock", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)))
                }
                Switch(
                    checked = gaming.isGamingModeActive,
                    onCheckedChange = { onToggleGaming() }
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Target FPS", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("${gaming.targetFps} FPS", color = Color(0xFF10B981), fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("RAM Freed", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("${gaming.memoryCleanedMb} MB", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }

            Button(
                onClick = { onToggleRecord() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = if (gaming.screenRecordingActive) Color(0xFFEF4444) else Color(0xFF6366F1)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(if (gaming.screenRecordingActive) Icons.Default.Stop else Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (gaming.screenRecordingActive) "Stop Screen Recording" else "Record Gameplay (1080p 60FPS)", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OrbStudioSection(
    customization: OrbCustomization,
    onUpdateCustomization: (OrbCustomization) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Holographic Orb Customization",
                style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
            )

            Text("Hologram Rendering Style", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OrbStyle.values().forEach { style ->
                    val isSelected = customization.style == style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFF00F0FF).copy(alpha = 0.15f) else Color(0xFF1E293B).copy(alpha = 0.5f))
                            .border(1.dp, if (isSelected) Color(0xFF00F0FF) else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { onUpdateCustomization(customization.copy(style = style)) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(style.title, color = if (isSelected) Color(0xFF00F0FF) else Color.White, fontWeight = FontWeight.SemiBold)
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Text("Orb Scale (${String.format(java.util.Locale.US, "%.1fx", customization.sizeScale)})", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Slider(
                value = customization.sizeScale,
                onValueChange = { onUpdateCustomization(customization.copy(sizeScale = it)) },
                valueRange = 0.6f..1.6f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00F0FF),
                    activeTrackColor = Color(0xFF00F0FF)
                )
            )
        }
    }
}

@Composable
fun SecurityOfflineSection(
    isVoiceAuth: Boolean,
    isPrivacy: Boolean,
    offlineModels: List<OfflineModel>,
    plugins: List<CustomPlugin>,
    onToggleVoiceAuth: () -> Boolean,
    onTogglePrivacy: () -> Boolean,
    onExecuteVoiceCommand: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Privacy & Biometrics
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Security & Privacy Shield", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Voice Biometric Authentication", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Requires acoustic voice print verification before executing sensitive device operations", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(checked = isVoiceAuth, onCheckedChange = { onToggleVoiceAuth() })
                }

                HorizontalDivider(color = Color(0xFF1E293B))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Incognito Privacy Mode", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Suspends all conversation transcripts, local caching, and telemetry", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(checked = isPrivacy, onCheckedChange = { onTogglePrivacy() })
                }

                Button(
                    onClick = { onExecuteVoiceCommand("delete my voice data") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Secure Wipe Local Audio & Transcripts", color = Color(0xFFEF4444), fontSize = 12.sp)
                }
            }
        }

        // Offline AI Models
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("On-Device Offline AI Models", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))

                offlineModels.forEach { model ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(model.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${model.sizeText} • ${model.type}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        if (model.isDownloaded) {
                            Badge(containerColor = Color(0xFF10B981)) { Text("READY", color = Color.White, fontSize = 9.sp) }
                        } else {
                            TextButton(onClick = { onExecuteVoiceCommand("download offline model") }) {
                                Text("Download", color = Color(0xFF00F0FF), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Community Marketplace
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Community Plugins & Skills", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))

                plugins.forEach { plugin ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(plugin.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("By ${plugin.author}", color = Color(0xFFF59E0B), fontSize = 11.sp)
                            Text(plugin.description, color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Badge(containerColor = Color(0xFF6366F1)) { Text("INSTALLED", color = Color.White, fontSize = 9.sp) }
                    }
                }
            }
        }
    }
}
