package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class QuickActionItem(
    val title: String,
    val command: String,
    val icon: ImageVector,
    val tint: Color = Color(0xFF00F0FF)
)

@Composable
fun QuickActions(
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        QuickActionItem("YouTube Search", "Open YouTube and search Arijit Singh", Icons.Default.SmartDisplay, Color(0xFFEF4444)),
        QuickActionItem("Play Song", "Open YouTube, search Arijit Singh, and play the first song", Icons.Default.PlayCircleFilled, Color(0xFF10B981)),
        QuickActionItem("Spotify Kesariya", "Open Spotify and play Kesariya", Icons.Default.Audiotrack, Color(0xFF1DB954)),
        QuickActionItem("Settings Wi-Fi", "Open Settings and go to Wi-Fi", Icons.Default.Wifi, Color(0xFF38BDF8)),
        QuickActionItem("Map Screen", "Show me the screen", Icons.Default.Radar, Color(0xFF00F0FF)),
        QuickActionItem("Find Search", "Find search on screen", Icons.Default.Search, Color(0xFFFACC15)),
        QuickActionItem("Click First", "Click first result", Icons.Default.TouchApp, Color(0xFF10B981)),
        QuickActionItem("Scroll Down", "Scroll down", Icons.Default.SwapVert, Color(0xFF818CF8)),
        QuickActionItem("Flashlight", "Turn on flashlight", Icons.Default.FlashlightOn, Color(0xFFF59E0B)),
        QuickActionItem("Battery Status", "Check battery status", Icons.Default.BatteryChargingFull, Color(0xFF34D399)),
        QuickActionItem("Set 7 AM Alarm", "Set alarm for 7 AM", Icons.Default.Alarm, Color(0xFFA855F7))
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Voice & Screen Action Shortcuts",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (action in actions) {
                SuggestionChip(
                    onClick = { onActionClick(action.command) },
                    label = {
                        Text(
                            text = action.title,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.title,
                            tint = action.tint,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = action.tint.copy(alpha = 0.35f)
                    )
                )
            }
        }
    }
}
