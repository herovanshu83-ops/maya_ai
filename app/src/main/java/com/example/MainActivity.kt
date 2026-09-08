package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.assistant.actions.ScreenCaptureService
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

enum class AppDestination(val title: String, val icon: ImageVector) {
    HOME("MAYA", Icons.Default.GraphicEq),
    HISTORY("History", Icons.Default.History),
    SETTINGS("Settings", Icons.Default.Settings)
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

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val captureService = ScreenCaptureService.getInstance(this)
            val success = captureService.startCapture(result.resultCode, result.data!!)
            if (success) {
                Toast.makeText(this, "Screen understanding active", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun requestScreenCapture() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        val intent = projectionManager?.createScreenCaptureIntent()
        if (intent != null) {
            screenCaptureLauncher.launch(intent)
        } else {
            Toast.makeText(this, "Screen capture service not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestInitialPermissions()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isSystemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                "light" -> false
                "system" -> isSystemDark
                else -> true
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                MainAppContainer(
                    viewModel = viewModel,
                    onRequestScreenCapture = { requestScreenCapture() }
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

@Composable
fun MainAppContainer(
    viewModel: MainViewModel,
    onRequestScreenCapture: () -> Unit = {}
) {
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }

    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()
    val orbTheme by viewModel.orbTheme.collectAsStateWithLifecycle()
    val audioLevel by viewModel.audioLevel.collectAsStateWithLifecycle()
    val transcript by viewModel.transcript.collectAsStateWithLifecycle()
    val response by viewModel.spokenResponse.collectAsStateWithLifecycle()
    val screenMap by viewModel.screenMap.collectAsStateWithLifecycle()
    val historyList by viewModel.commandHistory.collectAsStateWithLifecycle()
    val activeTask by viewModel.taskEngine.activeTask.collectAsStateWithLifecycle()

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val wakeWordOption by viewModel.wakeWordOption.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val voiceResponseEnabled by viewModel.voiceResponseEnabled.collectAsStateWithLifecycle()
    val ttsSpeed by viewModel.ttsSpeed.collectAsStateWithLifecycle()
    val confirmCriticalActions by viewModel.confirmCriticalActions.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "dest_anim"
        ) { destination ->
            when (destination) {
                AppDestination.HOME -> {
                    HomeScreen(
                        state = assistantState,
                        orbTheme = orbTheme,
                        audioLevel = audioLevel,
                        transcript = transcript,
                        response = response,
                        activeTask = activeTask,
                        screenMap = screenMap,
                        onToggleListen = { viewModel.toggleListening() },
                        onStopTask = { viewModel.taskEngine.cancelCurrentTask("User stopped task") },
                        onInspectScreen = { viewModel.mapScreen() },
                        onOpenTasks = { currentDestination = AppDestination.HISTORY },
                        onOpenSettings = { currentDestination = AppDestination.SETTINGS }
                    )
                }

                AppDestination.HISTORY -> {
                    HistoryScreen(
                        historyList = historyList,
                        onClearHistory = { viewModel.clearHistory() },
                        onBack = { currentDestination = AppDestination.HOME },
                        onExecuteCommand = { cmd ->
                            currentDestination = AppDestination.HOME
                            viewModel.processVoiceCommand(cmd)
                        }
                    )
                }

                AppDestination.SETTINGS -> {
                    SettingsScreen(
                        themeMode = themeMode,
                        onThemeModeChange = { viewModel.setThemeMode(it) },
                        wakeWordOption = wakeWordOption,
                        onWakeWordChange = { viewModel.setWakeWord(it) },
                        selectedLanguage = selectedLanguage,
                        onLanguageChange = { viewModel.setSelectedLanguage(it) },
                        voiceResponseEnabled = voiceResponseEnabled,
                        onVoiceResponseChange = { viewModel.setVoiceResponseEnabled(it) },
                        ttsSpeed = ttsSpeed,
                        onTtsSpeedChange = { viewModel.setTtsSpeed(it) },
                        screenUnderstandingEnabled = true,
                        onScreenUnderstandingChange = { /* Toggle */ },
                        confirmCriticalActions = confirmCriticalActions,
                        onConfirmCriticalActionsChange = { viewModel.setConfirmCriticalActions(it) },
                        onBack = { currentDestination = AppDestination.HOME }
                    )
                }
            }
        }
    }
}
