package com.example.assistant.audio

enum class ContinuousVoiceMode(val displayName: String, val description: String) {
    PUSH_TO_TALK("Push-to-Talk", "Tap the mic orb when you want to speak"),
    ALWAYS_ON("Always-On (Continuous)", "Automatically listens for your voice continuously in a conversational loop"),
    WAKE_WORD("Wake-Word Activation", "Stays active in the background and activates on 'Hey MAYA'")
}

enum class WakeWordOption(val displayName: String, val triggers: List<String>) {
    MAYA("Hey MAYA", listOf("hey maya", "ok maya", "maya")),
    JARVIS("Hey JARVIS", listOf("hey jarvis", "ok jarvis", "jarvis")),
    ASSISTANT("Hey Assistant", listOf("hey assistant", "ok assistant", "assistant"))
}

data class SpeechToTextConfig(
    val mode: ContinuousVoiceMode = ContinuousVoiceMode.ALWAYS_ON,
    val wakeWord: WakeWordOption = WakeWordOption.MAYA,
    val preferOffline: Boolean = true,
    val autoRestartOnTimeout: Boolean = true,
    val suppressSystemBeep: Boolean = true,
    val restartDelayMs: Long = 250L,
    val languageTag: String = "en-US"
)

sealed class SpeechRecognizerEvent {
    object ReadyForSpeech : SpeechRecognizerEvent()
    object BeginningOfSpeech : SpeechRecognizerEvent()
    data class RmsChanged(val normalizedRms: Float) : SpeechRecognizerEvent()
    data class PartialResult(val partialText: String) : SpeechRecognizerEvent()
    data class FinalResult(val recognizedText: String, val wakeWordDetected: Boolean) : SpeechRecognizerEvent()
    data class WakeWordTriggered(val trigger: String, val remainingCommand: String) : SpeechRecognizerEvent()
    data class Error(val errorCode: Int, val errorMessage: String, val isRecoverable: Boolean) : SpeechRecognizerEvent()
    object EndOfSpeech : SpeechRecognizerEvent()
    data class ModeChanged(val mode: ContinuousVoiceMode) : SpeechRecognizerEvent()
}
