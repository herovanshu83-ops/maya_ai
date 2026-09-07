package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MayaApplication
import com.example.assistant.actions.ActionExecutor
import com.example.assistant.actions.ScreenMapper
import com.example.assistant.actions.ScreenOverlay
import com.example.assistant.ai.GeminiService
import com.example.assistant.audio.ContinuousVoiceMode
import com.example.assistant.audio.SpeechRecognizerHelper
import com.example.assistant.audio.SpeechToTextConfig
import com.example.assistant.audio.TextToSpeechHelper
import com.example.assistant.audio.WakeWordOption
import com.example.assistant.core.AssistantState
import com.example.assistant.core.OrbTheme
import com.example.assistant.nlu.IntentClassifier
import com.example.assistant.nlu.IntentType
import com.example.data.local.entities.CommandHistory
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.RoutineEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MayaApplication).repository
    val userManager = com.example.assistant.user.UserManager.getInstance(application)
    val welcomeManager = com.example.assistant.user.WelcomeManager.getInstance(application)
    private val geminiService = GeminiService()
    val actionExecutor = ActionExecutor(application, repository, geminiService)
    val ultimateManager = com.example.assistant.ultimate.UltimateManager.getInstance(application)

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _orbTheme = MutableStateFlow(OrbTheme.CYBER_CYAN)
    val orbTheme: StateFlow<OrbTheme> = _orbTheme.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _spokenResponse = MutableStateFlow("Hi! I'm MAYA. I can see your screen, map elements, and speak every action.")
    val spokenResponse: StateFlow<String> = _spokenResponse.asStateFlow()

    private val _isContinuousMode = MutableStateFlow(false)
    val isContinuousMode: StateFlow<Boolean> = _isContinuousMode.asStateFlow()

    private val _continuousVoiceMode = MutableStateFlow(ContinuousVoiceMode.ALWAYS_ON)
    val continuousVoiceMode: StateFlow<ContinuousVoiceMode> = _continuousVoiceMode.asStateFlow()

    private val _wakeWordOption = MutableStateFlow(WakeWordOption.MAYA)
    val wakeWordOption: StateFlow<WakeWordOption> = _wakeWordOption.asStateFlow()

    private val _preferOfflineSpeech = MutableStateFlow(true)
    val preferOfflineSpeech: StateFlow<Boolean> = _preferOfflineSpeech.asStateFlow()

    private val _lastWakeWordEvent = MutableStateFlow<String?>(null)
    val lastWakeWordEvent: StateFlow<String?> = _lastWakeWordEvent.asStateFlow()

    private val _ttsSpeed = MutableStateFlow(1.0f)
    val ttsSpeed: StateFlow<Float> = _ttsSpeed.asStateFlow()

    private val _isBackgroundServiceRunning = MutableStateFlow(false)
    val isBackgroundServiceRunning: StateFlow<Boolean> = _isBackgroundServiceRunning.asStateFlow()

    private val _isOverlayRunning = MutableStateFlow(false)
    val isOverlayRunning: StateFlow<Boolean> = _isOverlayRunning.asStateFlow()

    private val _isAutoStartEnabled = MutableStateFlow(true)
    val isAutoStartEnabled: StateFlow<Boolean> = _isAutoStartEnabled.asStateFlow()

    val screenMap: StateFlow<ScreenMapper.ScreenMap?> = actionExecutor.screenMapper.screenMapFlow

    val visualFeedback: StateFlow<ScreenOverlay.VisualActionFeedback?> = actionExecutor.screenOverlay.visualFeedbackFlow

    val commandHistory: StateFlow<List<CommandHistory>> = repository.recentCommands
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routines: StateFlow<List<RoutineEntity>> = repository.allRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var speechRecognizer: SpeechRecognizerHelper? = null
    private var tts: TextToSpeechHelper? = null
    private var generativeModel: GenerativeModel? = null

    init {
        initAudioEngine()
        initFirebaseAI()
        refreshServiceStates()
    }

    private fun initFirebaseAI() {
        try {
            generativeModel = Firebase.ai.generativeModel("gemini-2.5-flash")
        } catch (e: Exception) {
            Log.w("MainViewModel", "Firebase AI SDK initialization failed: ${e.message}")
        }
    }

    fun refreshServiceStates() {
        val app = getApplication<Application>()
        _isBackgroundServiceRunning.value = com.example.assistant.services.MayaForegroundService.isRunning
        _isOverlayRunning.value = com.example.assistant.services.MayaOverlayService.isRunning
        val prefs = app.getSharedPreferences("maya_prefs", android.content.Context.MODE_PRIVATE)
        _isAutoStartEnabled.value = prefs.getBoolean("auto_start_background", true)
    }

    fun toggleBackgroundService() {
        val app = getApplication<Application>()
        if (com.example.assistant.services.MayaForegroundService.isRunning) {
            com.example.assistant.services.MayaForegroundService.stop(app)
            _isBackgroundServiceRunning.value = false
        } else {
            com.example.assistant.services.MayaForegroundService.start(app)
            _isBackgroundServiceRunning.value = true
        }
    }

    fun toggleOverlayService() {
        val app = getApplication<Application>()
        val overlayHelper = com.example.utils.OverlayPermissionHelper(app)
        if (com.example.assistant.services.MayaOverlayService.isRunning) {
            com.example.assistant.services.MayaOverlayService.stop(app)
            _isOverlayRunning.value = false
        } else {
            if (overlayHelper.hasOverlayPermission()) {
                com.example.assistant.services.MayaOverlayService.start(app)
                _isOverlayRunning.value = true
            }
        }
    }

    fun toggleAutoStart() {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("maya_prefs", android.content.Context.MODE_PRIVATE)
        val newState = !_isAutoStartEnabled.value
        prefs.edit().putBoolean("auto_start_background", newState).apply()
        _isAutoStartEnabled.value = newState
    }

    private fun initAudioEngine() {
        tts = TextToSpeechHelper(getApplication())

        speechRecognizer = SpeechRecognizerHelper(
            context = getApplication(),
            onResult = { text ->
                _audioLevel.value = 0f
                _transcript.value = text
                processVoiceCommand(text)
            },
            onPartialResult = { partial ->
                _transcript.value = partial
            },
            onRmsChanged = { level ->
                if (_assistantState.value == AssistantState.LISTENING) {
                    _audioLevel.value = level
                }
            },
            onError = { error ->
                _audioLevel.value = 0f
                if (!_isContinuousMode.value) {
                    _assistantState.value = AssistantState.IDLE
                }
            },
            onReady = {
                _assistantState.value = AssistantState.LISTENING
                com.example.assistant.services.MayaForegroundService.updateStatus(
                    getApplication(),
                    if (_isContinuousMode.value) "MAYA: Continuous listening active..." else "MAYA: Listening..."
                )
            },
            onWakeWordDetected = { trigger, command ->
                _lastWakeWordEvent.value = "Trigger: '$trigger'"
                if (command.isNotBlank()) {
                    _transcript.value = command
                }
            }
        )
    }

    fun toggleListening() {
        if (_assistantState.value == AssistantState.LISTENING) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun startListening() {
        tts?.stop()
        _transcript.value = ""
        _assistantState.value = AssistantState.LISTENING
        com.example.assistant.services.MayaOverlayService.update(getApplication(), "Listening...", "listening")
        speechRecognizer?.startListening()
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _assistantState.value = AssistantState.IDLE
        _audioLevel.value = 0f
        com.example.assistant.services.MayaOverlayService.update(getApplication(), "MAYA Ready", "info")
    }

    fun processVoiceCommand(command: String) {
        if (command.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _assistantState.value = AssistantState.THINKING
                com.example.assistant.services.MayaOverlayService.update(getApplication(), "Thinking: $command", "thinking")
                delay(200)

                val parsedIntent = IntentClassifier.classify(command)

                // Use Firebase Generative AI SDK to stream responses once speech has been successfully converted to text
                val model = generativeModel
                if (model != null && parsedIntent.type != IntentType.OPEN_APP && parsedIntent.type != IntentType.SYSTEM_CONTROL) {
                    _assistantState.value = AssistantState.SPEAKING
                    var fullResponse = ""
                    try {
                        val responseFlow = model.generateContentStream(command)
                        responseFlow.collect { chunk ->
                            val chunkText = chunk.text ?: ""
                            fullResponse += chunkText
                            _spokenResponse.value = fullResponse
                            com.example.assistant.services.MayaOverlayService.update(getApplication(), fullResponse, "speaking")
                        }

                        try {
                            repository.logCommand(
                                text = command,
                                intentType = parsedIntent.type.name,
                                response = fullResponse,
                                isSuccess = true
                            )
                        } catch (e: Exception) {
                            // Ignore DB logging failure
                        }

                        speakResponse(fullResponse)
                        return@launch
                    } catch (e: Exception) {
                        Log.w("MainViewModel", "Firebase AI streaming failed, falling back to ActionExecutor: ${e.message}")
                    }
                }

                _assistantState.value = AssistantState.EXECUTING
                com.example.assistant.services.MayaOverlayService.update(getApplication(), "Executing...", "speaking")
                val result = actionExecutor.execute(parsedIntent)

                try {
                    repository.logCommand(
                        text = command,
                        intentType = parsedIntent.type.name,
                        response = result.spokenResponse,
                        isSuccess = result.isSuccess
                    )
                } catch (e: Exception) {
                    // Ignore DB logging failure
                }

                _spokenResponse.value = result.spokenResponse
                com.example.assistant.services.MayaOverlayService.update(
                    getApplication(),
                    result.spokenResponse,
                    if (result.isSuccess) "success" else "error"
                )

                // Speak answer using TTS
                speakResponse(result.spokenResponse)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error processing voice command: ${e.message}", e)
                _assistantState.value = AssistantState.IDLE
                _spokenResponse.value = "Sorry, I encountered an error processing that request."
                speakResponse("Sorry, I encountered an error processing that request.")
            }
        }
    }

    fun mapScreen() {
        processVoiceCommand("show me the screen")
    }

    fun executeQuickAction(query: String) {
        processVoiceCommand(query)
    }

    private fun speakResponse(text: String) {
        _assistantState.value = AssistantState.SPEAKING
        _audioLevel.value = 0.5f

        // Pause speech recognizer while speaking to prevent feedback echo
        speechRecognizer?.pauseForAssistantSpeaking()

        tts?.speak(
            text = text,
            onStart = {
                _assistantState.value = AssistantState.SPEAKING
            },
            onDone = {
                _audioLevel.value = 0f
                _assistantState.value = AssistantState.IDLE

                // Continuous listening loop: resume speech recognition cleanly
                if (_isContinuousMode.value) {
                    speechRecognizer?.resumeAfterAssistantSpeaking(delayMs = 400L)
                }
            }
        )
    }

    fun stopSpeaking() {
        tts?.stop()
        _assistantState.value = AssistantState.IDLE
        _audioLevel.value = 0f
        if (_isContinuousMode.value) {
            speechRecognizer?.resumeAfterAssistantSpeaking(delayMs = 200L)
        }
    }

    fun toggleContinuousMode() {
        setContinuousMode(!_isContinuousMode.value)
    }

    fun setContinuousMode(enabled: Boolean) {
        _isContinuousMode.value = enabled
        val targetMode = if (enabled) _continuousVoiceMode.value else ContinuousVoiceMode.PUSH_TO_TALK
        speechRecognizer?.setContinuousMode(targetMode)

        if (enabled) {
            // Auto start background foreground service for persistent mic capture
            if (!com.example.assistant.services.MayaForegroundService.isRunning) {
                com.example.assistant.services.MayaForegroundService.start(getApplication())
                _isBackgroundServiceRunning.value = true
            }
            startListening()
        } else {
            stopListening()
        }
    }

    fun setContinuousVoiceMode(mode: ContinuousVoiceMode) {
        _continuousVoiceMode.value = mode
        if (_isContinuousMode.value) {
            speechRecognizer?.setContinuousMode(mode)
        }
    }

    fun setWakeWord(wakeWord: WakeWordOption) {
        _wakeWordOption.value = wakeWord
        speechRecognizer?.setWakeWord(wakeWord)
    }

    fun setPreferOffline(prefer: Boolean) {
        _preferOfflineSpeech.value = prefer
        speechRecognizer?.setPreferOffline(prefer)
    }

    fun isDeviceOfflineRecognitionAvailable(): Boolean {
        return speechRecognizer?.isDeviceOfflineRecognitionAvailable() ?: false
    }

    fun setOrbTheme(theme: OrbTheme) {
        _orbTheme.value = theme
    }

    fun setTtsSpeed(speed: Float) {
        _ttsSpeed.value = speed
        tts?.speechRate = speed
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }

    fun addMemory(key: String, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveMemory(key, value)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllMemories()
        }
    }

    fun runRoutine(triggerPhrase: String) {
        processVoiceCommand("run $triggerPhrase")
    }

    fun createRoutine(name: String, trigger: String, actions: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveRoutine(name, trigger, actions)
        }
    }

    fun deleteRoutine(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRoutine(id)
        }
    }

    fun wipeAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
            repository.clearAllMemories()
            _spokenResponse.value = "All activity logs and stored memories have been cleared."
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        tts?.shutdown()
    }
}
