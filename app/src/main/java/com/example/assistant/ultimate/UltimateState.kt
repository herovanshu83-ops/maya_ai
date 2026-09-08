package com.example.assistant.ultimate

import androidx.compose.ui.graphics.Color

enum class MayaPersonality(
    val title: String,
    val description: String,
    val systemPromptModifier: String,
    val sampleGreeting: String
) {
    PROFESSIONAL(
        "Professional",
        "Formal, precise, efficient, executive tone",
        "Adopt a formal, courteous, and highly precise executive tone. Be concise and authoritative.",
        "Good day. All systems operational. How may I direct your schedule and operations?"
    ),
    FRIENDLY(
        "Friendly",
        "Warm, casual, encouraging and cheerful",
        "Adopt a warm, cheerful, and encouraging tone. Be supportive and friendly like a trusted companion.",
        "Hey there! It's great to talk to you. What exciting thing are we doing today?"
    ),
    SARCASTIC(
        "Sarcastic",
        "Witty, humorous, Tony Stark-style banter",
        "Adopt a witty, playful, and dryly sarcastic tone like Jarvis or Tony Stark. Keep it clever and banter-filled without being rude.",
        "Ah, my favorite human returns. Let's see what universe-saving task you have for me today."
    ),
    MINIMALIST(
        "Minimalist",
        "Ultra-short, direct, zero fluff",
        "Adopt an ultra-concise, minimalist tone. Give maximum information in the fewest words possible. No filler words.",
        "MAYA ready. Command?"
    ),
    MOTIVATIONAL(
        "Motivational",
        "Inspiring, energetic, champion mindset",
        "Adopt an inspiring, high-energy, motivational coach persona. Encourage the user and celebrate their momentum.",
        "Let's make today extraordinary! You've got the power to crush every goal. What's first?"
    ),
    NERDY(
        "Nerdy",
        "Tech-savvy, geeky, algorithmic references",
        "Adopt a deeply geeky, tech-savvy persona with subtle sci-fi, quantum computing, and code references.",
        "Quantum core synchronized at 100% clock speed. Standing by for your algorithmic query."
    )
}

enum class MayaMood(val title: String, val emoji: String, val responseStyle: String) {
    HAPPY("Happy", "😊", "Upbeat and celebratory"),
    SAD("Down / Sad", "🥺", "Compassionate, soothing and gentle"),
    ANGRY("Frustrated", "😤", "Calm, de-escalating and solution-focused"),
    TIRED("Tired / Low Energy", "😴", "Soft, gentle and light on cognitive load"),
    STRESSED("Stressed", "😰", "Reassuring, structured and offering relaxation"),
    NEUTRAL("Balanced", "✨", "Attentive and steady")
}

data class SmartDevice(
    val id: String,
    val name: String,
    val room: String,
    val platform: String, // Google Home, Alexa, Hue, etc.
    val type: DeviceType,
    var isOn: Boolean,
    var value: String = "" // "72°F", "80%", "Locked"
)

enum class DeviceType {
    LIGHT, THERMOSTAT, LOCK, PLUG, FAN, SPEAKER, GARAGE, BLINDS, VACUUM
}

data class VehicleStatus(
    val model: String = "Tesla Model 3 Performance",
    val batteryPercent: Int = 84,
    val estimatedRangeMiles: Int = 265,
    var isLocked: Boolean = true,
    var climateOn: Boolean = false,
    var targetTempFahrenheit: Int = 70,
    var trunkOpen: Boolean = false,
    var frunkOpen: Boolean = false,
    val location: String = "Home Garage • 100% GPS Lock"
)

data class HealthData(
    var stepsToday: Int = 8420,
    val stepGoal: Int = 10000,
    val heartRateBpm: Int = 72,
    val sleepHours: Float = 7.5f,
    val sleepQualityScore: Int = 89,
    var waterLiters: Float = 2.1f,
    val caloriesBurned: Int = 1920,
    val stressPercent: Int = 22,
    val healthConnectSynced: Boolean = true
)

data class FinancialData(
    val monthlyBudget: Double = 3500.0,
    var spentThisMonth: Double = 1840.50,
    val nextBill: String = "Internet Fiber ($75.00) in 3 days",
    val btcPrice: String = "$91,420.00",
    val ethPrice: String = "$3,450.00",
    val solPrice: String = "$210.00"
)

data class GamingState(
    var isGamingModeActive: Boolean = false,
    var dndEnabled: Boolean = false,
    var performanceBoost: Boolean = true,
    var screenRecordingActive: Boolean = false,
    val targetFps: Int = 120,
    val memoryCleanedMb: Int = 1420
)

data class OfflineModel(
    val id: String,
    val name: String,
    val type: String,
    val sizeText: String,
    var isDownloaded: Boolean = false,
    var isInstalled: Boolean = false
)

data class CustomPlugin(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    var isEnabled: Boolean = true
)

data class OrbCustomization(
    val sizeScale: Float = 1.0f,
    val speedMultiplier: Float = 1.0f,
    val style: OrbStyle = OrbStyle.CYBER_CORE,
    val ringCount: Int = 3,
    val particleDensity: Int = 2, // 1: Low, 2: Med, 3: High
    val glowIntensity: Float = 1.0f
)

enum class OrbStyle(val title: String) {
    CYBER_CORE("Cyber Core"),
    AURORA_WAVE("Aurora Wave"),
    GALAXY_SPIRAL("Galaxy Spiral"),
    MINIMAL_RING("Minimalist Ring"),
    HYPER_PULSE("Hyper Pulse")
}
