package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.RoutineEntity

@Composable
fun RoutinesScreen(
    routines: List<RoutineEntity>,
    onRunRoutine: (String) -> Unit,
    onDeleteRoutine: (Long) -> Unit,
    onCreateRoutine: (String, String, List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var routineName by remember { mutableStateOf("") }
    var routineTrigger by remember { mutableStateOf("") }
    var routineActions by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Automation Routines",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Multi-action sequences triggered by a single phrase",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }

            IconButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Add Routine", tint = Color(0xFF00F0FF))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (routines.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No routines configured yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(routines, key = { it.id }) { routine ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = routine.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Trigger: \"${routine.triggerPhrase}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF38BDF8)
                                    )
                                }

                                Button(
                                    onClick = { onRunRoutine(routine.triggerPhrase) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF))
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF070A13), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Run", color = Color(0xFF070A13), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Actions:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )

                            val actions = routine.actionsJson.split(";").filter { it.isNotBlank() }
                            for ((idx, action) in actions.withIndex()) {
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${idx + 1}. ", style = MaterialTheme.typography.bodySmall, color = Color(0xFF00F0FF))
                                    Text(action, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Voice Routine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = routineName,
                        onValueChange = { routineName = it },
                        label = { Text("Routine Name (e.g. Gym Mode)") }
                    )
                    OutlinedTextField(
                        value = routineTrigger,
                        onValueChange = { routineTrigger = it },
                        label = { Text("Voice Trigger (e.g. start workout)") }
                    )
                    OutlinedTextField(
                        value = routineActions,
                        onValueChange = { routineActions = it },
                        label = { Text("Actions (separated by semicolon ';')") },
                        placeholder = { Text("Set volume to 80%; Open Spotify") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (routineName.isNotBlank() && routineTrigger.isNotBlank() && routineActions.isNotBlank()) {
                            val actionList = routineActions.split(";").map { it.trim() }.filter { it.isNotBlank() }
                            onCreateRoutine(routineName, routineTrigger, actionList)
                            routineName = ""
                            routineTrigger = ""
                            routineActions = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
