package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.assistant.nlu.IntentClassifier
import com.example.assistant.nlu.IntentType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MAYA", appName)
    }

    @Test
    fun `verify flashlight voice commands`() {
        val intentOn = IntentClassifier.classify("Turn on flashlight")
        assertEquals(IntentType.FLASHLIGHT, intentOn.type)
        assertEquals("on", intentOn.action)

        val intentOff = IntentClassifier.classify("Turn off torch")
        assertEquals(IntentType.FLASHLIGHT, intentOff.type)
        assertEquals("off", intentOff.action)
    }

    @Test
    fun `verify app launcher voice commands`() {
        val intent = IntentClassifier.classify("Open YouTube")
        assertEquals(IntentType.OPEN_APP, intent.type)
        assertEquals("YouTube", intent.target)
    }

    @Test
    fun `verify alarm and timer voice commands`() {
        val alarm = IntentClassifier.classify("Set alarm for 7:30 AM")
        assertEquals(IntentType.ALARM, alarm.type)

        val timer = IntentClassifier.classify("Set timer for 10 minutes")
        assertEquals(IntentType.TIMER, timer.type)
        assertEquals("10", timer.parameters["duration"])
    }

    @Test
    fun `verify phone call and messaging commands`() {
        val call = IntentClassifier.classify("Call Mom")
        assertEquals(IntentType.CALL, call.type)
        assertEquals("Mom", call.target)

        val text = IntentClassifier.classify("Send message to Mom saying I'm on my way")
        assertEquals(IntentType.SEND_MESSAGE, text.type)
        assertEquals("Mom", text.target)
    }

    @Test
    fun `verify ultimate assistant voice commands`() {
        val personality = IntentClassifier.classify("Make MAYA sarcastic")
        assertEquals(IntentType.PERSONALITY_CONTROL, personality.type)

        val mood = IntentClassifier.classify("How are you feeling today?")
        assertEquals(IntentType.MOOD_CONTROL, mood.type)

        val smartHome = IntentClassifier.classify("Turn off all lights")
        assertEquals(IntentType.SMART_HOME_CONTROL, smartHome.type)

        val vehicle = IntentClassifier.classify("unlock my tesla")
        assertEquals(IntentType.VEHICLE_CONTROL, vehicle.type)

        val health = IntentClassifier.classify("track my health")
        assertEquals(IntentType.HEALTH_CONTROL, health.type)

        val gaming = IntentClassifier.classify("gaming mode on")
        assertEquals(IntentType.GAMING_CONTROL, gaming.type)

        val security = IntentClassifier.classify("Delete my voice data")
        assertEquals(IntentType.SECURITY_CONTROL, security.type)
    }

    @Test
    fun `verify encryption and creator credit`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val userManager = com.example.assistant.user.UserManager.getInstance(context)
        assertEquals("🌟 MADE BY MANI", userManager.getCreatorCredit())

        val encryptionManager = com.example.assistant.security.EncryptionManager.getInstance(context)
        val plainText = "AIzaSySecretApiKey12345"
        val encrypted = encryptionManager.encryptApiKey(plainText)
        assert(encrypted.isNotEmpty())
        val decrypted = encryptionManager.decryptApiKey(encrypted)
        assertEquals(plainText, decrypted)
    }

    @Test
    fun `verify continuous speech to text configuration`() {
        val config = com.example.assistant.audio.SpeechToTextConfig(
            mode = com.example.assistant.audio.ContinuousVoiceMode.ALWAYS_ON,
            wakeWord = com.example.assistant.audio.WakeWordOption.MAYA,
            preferOffline = true
        )
        assertEquals(com.example.assistant.audio.ContinuousVoiceMode.ALWAYS_ON, config.mode)
        assertEquals(com.example.assistant.audio.WakeWordOption.MAYA, config.wakeWord)
        assert(config.wakeWord.triggers.contains("hey maya"))
        assert(config.wakeWord.triggers.contains("maya"))
    }
}
