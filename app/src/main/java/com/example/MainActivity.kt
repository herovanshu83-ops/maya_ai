package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.assistant.services.MayaForegroundService
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

enum class NavTab(val title: String, val icon: ImageVector) {
    HOME("Assistant", Icons.Default.GraphicEq),
    ULTIMATE("Ultimate", Icons.Default.AutoAwesome),
    TOOLS("Device", Icons.Default.Dashboard),
    ROUTINES("Routines", Icons.Default.AutoFixHigh),
    HISTORY("History", Icons.Default.History),
    PRIVACY("Privacy", Icons.Default.Security)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!micGranted) {
            Toast.makeText(this, "Microphone access is required for voice recognition", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestInitialPermissions()

        setContent {
            MyApplicationTheme {
                MainAppContainer(
                    viewModel = viewModel,
                    onRequestPermissions = { requestInitialPermissions() }
                )
            }
        }
    }

    private fun requestInitialPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        permissionLauncher.launch(permissions.toTypedArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit
) {
    var currentTab by remember { mutableStateOf(NavTab.HOME) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()
    val orbTheme by viewModel.orbTheme.collectAsStateWithLifecycle()
    val audioLevel by viewModel.audioLevel.collectAsStateWithLifecycle()
    val transcript by viewModel.transcript.collectAsStateWithLifecycle()
    val response by viewModel.spokenResponse.collectAsStateWithLifecycle()
    val isContinuousMode by viewModel.isContinuousMode.collectAsStateWithLifecycle()
    val ttsSpeed by viewModel.ttsSpeed.collectAsStateWithLifecycle()
    val screenMap by viewModel.screenMap.collectAsStateWithLifecycle()
    val visualFeedback by viewModel.visualFeedback.collectAsStateWithLifecycle()
    val historyList by viewModel.commandHistory.collectAsStateWithLifecycle()
    val memoriesList by viewModel.memories.collectAsStateWithLifecycle()
    val routinesList by viewModel.routines.collectAsStateWithLifecycle()
    val isBackgroundServiceRunning by viewModel.isBackgroundServiceRunning.collectAsStateWithLifecycle()
    val isOverlayRunning by viewModel.isOverlayRunning.collectAsStateWithLifecycle()
    val isAutoStartEnabled by viewModel.isAutoStartEnabled.collectAsStateWithLifecycle()
    val orbCustomization by viewModel.ultimateManager.orbCustomization.collectAsStateWithLifecycle()

    val continuousVoiceMode by viewModel.continuousVoiceMode.collectAsStateWithLifecycle()
    val wakeWordOption by viewModel.wakeWordOption.collectAsStateWithLifecycle()
    val preferOfflineSpeech by viewModel.preferOfflineSpeech.collectAsStateWithLifecycle()

    val currentUser by viewModel.userManager.currentUser.collectAsStateWithLifecycle()
    val isGuestMode by viewModel.userManager.isGuestMode.collectAsStateWithLifecycle()
    val showWelcome by viewModel.welcomeManager.showWelcome.collectAsStateWithLifecycle()
    val welcomeMessage by viewModel.welcomeManager.welcomeMessage.collectAsStateWithLifecycle()
    val creatorCredit by viewModel.welcomeManager.creatorCredit.collectAsStateWithLifecycle()
    val dailyMessage by viewModel.welcomeManager.dailyMessage.collectAsStateWithLifecycle()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showAuthScreen by remember { mutableStateOf(false) }
    var showApiKeyScreen by remember { mutableStateOf(false) }

    LaunchedEffect(showSettingsSheet) {
        if (showSettingsSheet) {
            viewModel.refreshServiceStates()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF070A13),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0A0F1D),
                contentColor = Color(0xFF00F0FF),
                tonalElevation = 8.dp
            ) {
                for (tab in NavTab.values()) {
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                color = if (isSelected) Color(0xFF00F0FF) else Color(0xFF64748B)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00F0FF),
                            selectedTextColor = Color(0xFF00F0FF),
                            indicatorColor = Color(0xFF00F0FF).copy(alpha = 0.15f),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavTab.HOME -> {
                    HomeScreen(
                        state = assistantState,
                        orbTheme = orbTheme,
                        audioLevel = audioLevel,
                        transcript = transcript,
                        response = response,
                        screenMap = screenMap,
                        visualFeedback = visualFeedback,
                        isContinuousMode = isContinuousMode,
                        onToggleListen = { viewModel.toggleListening() },
                        onToggleContinuous = { viewModel.toggleContinuousMode() },
                        onMapScreen = { viewModel.mapScreen() },
                        onExecuteCommand = { viewModel.processVoiceCommand(it) },
                        onStopSpeech = { viewModel.stopSpeaking() },
                        onOpenSettings = { showSettingsSheet = true },
                        orbCustomization = orbCustomization,
                        user = currentUser,
                        creatorCredit = creatorCredit,
                        dailyMessage = dailyMessage,
                        onOpenProfile = { showProfileDialog = true }
                    )
                }
                NavTab.ULTIMATE -> {
                    UltimateScreen(
                        ultimateManager = viewModel.ultimateManager,
                        onExecuteVoiceCommand = { viewModel.processVoiceCommand(it) }
                    )
                }
                NavTab.TOOLS -> {
                    DeviceToolsScreen(
                        onExecuteVoiceCommand = { viewModel.processVoiceCommand(it) }
                    )
                }
                NavTab.HISTORY -> {
                    HistoryScreen(
                        historyList = historyList,
                        memoriesList = memoriesList,
                        onClearHistory = { viewModel.clearHistory() },
                        onDeleteMemory = { viewModel.deleteMemory(it) },
                        onAddMemory = { k, v -> viewModel.addMemory(k, v) },
                        onClearAllMemories = { viewModel.clearAllMemories() },
                        onExecuteCommand = { viewModel.processVoiceCommand(it) }
                    )
                }
                NavTab.ROUTINES -> {
                    RoutinesScreen(
                        routines = routinesList,
                        onRunRoutine = { viewModel.runRoutine(it) },
                        onDeleteRoutine = { viewModel.deleteRoutine(it) },
                        onCreateRoutine = { n, t, a -> viewModel.createRoutine(n, t, a) }
                    )
                }
                NavTab.PRIVACY -> {
                    PrivacyScreen(
                        onRequestAllPermissions = onRequestPermissions,
                        onWipeAllData = { viewModel.wipeAllData() }
                    )
                }
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = Color(0xFF070A13),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF334155)) }
        ) {
            SettingsScreen(
                currentOrbTheme = orbTheme,
                onSelectOrbTheme = { viewModel.setOrbTheme(it) },
                ttsSpeed = ttsSpeed,
                onTtsSpeedChange = { viewModel.setTtsSpeed(it) },
                isContinuousMode = isContinuousMode,
                onContinuousModeToggle = { viewModel.setContinuousMode(it) },
                isBackgroundServiceRunning = isBackgroundServiceRunning,
                onToggleBackgroundService = { viewModel.toggleBackgroundService() },
                isOverlayRunning = isOverlayRunning,
                onToggleOverlayService = { viewModel.toggleOverlayService() },
                isAutoStartEnabled = isAutoStartEnabled,
                onToggleAutoStart = { viewModel.toggleAutoStart() },
                continuousVoiceMode = continuousVoiceMode,
                onContinuousVoiceModeChange = { viewModel.setContinuousVoiceMode(it) },
                wakeWordOption = wakeWordOption,
                onWakeWordChange = { viewModel.setWakeWord(it) },
                preferOfflineSpeech = preferOfflineSpeech,
                onPreferOfflineSpeechChange = { viewModel.setPreferOffline(it) },
                isDeviceOfflineAvailable = viewModel.isDeviceOfflineRecognitionAvailable(),
                onOpenApiKeys = {
                    showSettingsSheet = false
                    showApiKeyScreen = true
                },
                onOpenProfile = {
                    showSettingsSheet = false
                    showProfileDialog = true
                },
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }

    if (showWelcome) {
        com.example.ui.components.WelcomeDialog(
            message = welcomeMessage,
            onDismiss = { viewModel.welcomeManager.dismissWelcome() }
        )
    }

    if (showProfileDialog) {
        com.example.ui.screens.UserProfileDialog(
            user = currentUser,
            isGuest = isGuestMode,
            userManager = viewModel.userManager,
            onOpenApiKeys = {
                showProfileDialog = false
                showApiKeyScreen = true
            },
            onOpenAuth = {
                showProfileDialog = false
                showAuthScreen = true
            },
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showAuthScreen) {
        Dialog(
            onDismissRequest = { showAuthScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            com.example.ui.screens.AuthScreen(
                userManager = viewModel.userManager,
                onAuthSuccess = { showAuthScreen = false }
            )
        }
    }

    if (showApiKeyScreen) {
        Dialog(
            onDismissRequest = { showApiKeyScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            com.example.ui.screens.ApiKeyManagementScreen(
                userManager = viewModel.userManager,
                onBack = { showApiKeyScreen = false }
            )
        }
    }
}
