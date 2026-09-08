package com.example.assistant.ultimate

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UltimateManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("maya_ultimate_prefs", Context.MODE_PRIVATE)

    // Personality & Mood
    private val _personality = MutableStateFlow(loadPersonality())
    val personality: StateFlow<MayaPersonality> = _personality.asStateFlow()

    private val _userMood = MutableStateFlow(MayaMood.HAPPY)
    val userMood: StateFlow<MayaMood> = _userMood.asStateFlow()

    // Smart Home Devices
    private val _devices = MutableStateFlow(initDefaultDevices())
    val devices: StateFlow<List<SmartDevice>> = _devices.asStateFlow()

    // Vehicle (Android Auto / Tesla)
    private val _vehicle = MutableStateFlow(VehicleStatus())
    val vehicle: StateFlow<VehicleStatus> = _vehicle.asStateFlow()

    // Health & Fitness
    private val _health = MutableStateFlow(HealthData())
    val health: StateFlow<HealthData> = _health.asStateFlow()

    // Financial Data
    private val _finance = MutableStateFlow(FinancialData())
    val finance: StateFlow<FinancialData> = _finance.asStateFlow()

    // Gaming Mode
    private val _gaming = MutableStateFlow(GamingState())
    val gaming: StateFlow<GamingState> = _gaming.asStateFlow()

    // Offline Models
    private val _offlineModels = MutableStateFlow(initDefaultModels())
    val offlineModels: StateFlow<List<OfflineModel>> = _offlineModels.asStateFlow()

    // Community Plugins
    private val _plugins = MutableStateFlow(initDefaultPlugins())
    val plugins: StateFlow<List<CustomPlugin>> = _plugins.asStateFlow()

    // Orb Customization
    private val _orbCustomization = MutableStateFlow(loadOrbCustomization())
    val orbCustomization: StateFlow<OrbCustomization> = _orbCustomization.asStateFlow()

    // Security & Voice Auth
    private val _isVoiceAuthEnabled = MutableStateFlow(prefs.getBoolean("voice_auth_enabled", false))
    val isVoiceAuthEnabled: StateFlow<Boolean> = _isVoiceAuthEnabled.asStateFlow()

    private val _isPrivacyMode = MutableStateFlow(prefs.getBoolean("privacy_mode", false))
    val isPrivacyMode: StateFlow<Boolean> = _isPrivacyMode.asStateFlow()

    // 1. Personality
    fun setPersonality(newPersonality: MayaPersonality) {
        _personality.value = newPersonality
        prefs.edit().putString("personality", newPersonality.name).apply()
    }

    private fun loadPersonality(): MayaPersonality {
        val name = prefs.getString("personality", MayaPersonality.SARCASTIC.name)
        return try {
            MayaPersonality.valueOf(name ?: MayaPersonality.SARCASTIC.name)
        } catch (e: Exception) {
            MayaPersonality.SARCASTIC
        }
    }

    fun setMood(mood: MayaMood) {
        _userMood.value = mood
    }

    // 2. Smart Home Controls
    fun toggleDevice(deviceId: String): Boolean {
        val currentList = _devices.value.map { device ->
            if (device.id == deviceId) {
                device.copy(isOn = !device.isOn)
            } else {
                device
            }
        }
        _devices.value = currentList
        val target = currentList.find { it.id == deviceId }
        return target?.isOn ?: false
    }

    fun setAllLights(turnOn: Boolean) {
        _devices.value = _devices.value.map { device ->
            if (device.type == DeviceType.LIGHT) {
                device.copy(isOn = turnOn)
            } else {
                device
            }
        }
    }

    fun applyScene(sceneName: String) {
        when (sceneName.lowercase()) {
            "movie", "movie mode" -> {
                _devices.value = _devices.value.map {
                    when (it.type) {
                        DeviceType.LIGHT -> it.copy(isOn = it.room.contains("Living", true), value = "20% Warm Dim")
                        DeviceType.BLINDS -> it.copy(isOn = false, value = "Closed")
                        DeviceType.SPEAKER -> it.copy(isOn = true, value = "Spatial Cinema")
                        else -> it
                    }
                }
            }
            "vacation", "vacation mode" -> {
                _devices.value = _devices.value.map {
                    when (it.type) {
                        DeviceType.LIGHT -> it.copy(isOn = false)
                        DeviceType.LOCK -> it.copy(isOn = true, value = "Deadbolt Armed")
                        DeviceType.THERMOSTAT -> it.copy(value = "64°F Eco")
                        DeviceType.GARAGE -> it.copy(isOn = false, value = "Locked Closed")
                        else -> it
                    }
                }
            }
            "study", "study mode" -> {
                _devices.value = _devices.value.map {
                    when (it.type) {
                        DeviceType.LIGHT -> it.copy(isOn = true, value = "100% Daylight 5000K")
                        DeviceType.SPEAKER -> it.copy(isOn = true, value = "Lo-Fi Focus Beats")
                        else -> it
                    }
                }
            }
        }
    }

    fun setThermostatTemp(temp: String) {
        _devices.value = _devices.value.map {
            if (it.type == DeviceType.THERMOSTAT) {
                it.copy(isOn = true, value = temp)
            } else {
                it
            }
        }
    }

    // 3. Vehicle / Tesla
    fun toggleTeslaLock(): Boolean {
        val newState = !_vehicle.value.isLocked
        _vehicle.value = _vehicle.value.copy(isLocked = newState)
        return newState
    }

    fun toggleTeslaClimate(): Boolean {
        val newState = !_vehicle.value.climateOn
        _vehicle.value = _vehicle.value.copy(climateOn = newState)
        return newState
    }

    fun toggleTeslaFrunk(): Boolean {
        val newState = !_vehicle.value.frunkOpen
        _vehicle.value = _vehicle.value.copy(frunkOpen = newState)
        return newState
    }

    fun toggleTeslaTrunk(): Boolean {
        val newState = !_vehicle.value.trunkOpen
        _vehicle.value = _vehicle.value.copy(trunkOpen = newState)
        return newState
    }

    // 4. Health & Fitness
    fun logWater(amountLiters: Float = 0.25f) {
        val updated = _health.value.copy(waterLiters = (_health.value.waterLiters + amountLiters))
        _health.value = updated
    }

    fun addSteps(steps: Int) {
        val updated = _health.value.copy(stepsToday = _health.value.stepsToday + steps)
        _health.value = updated
    }

    // 5. Gaming Mode
    fun toggleGamingMode(): Boolean {
        val active = !_gaming.value.isGamingModeActive
        _gaming.value = _gaming.value.copy(
            isGamingModeActive = active,
            dndEnabled = active,
            performanceBoost = active
        )
        return active
    }

    fun toggleScreenRecording(): Boolean {
        val rec = !_gaming.value.screenRecordingActive
        _gaming.value = _gaming.value.copy(screenRecordingActive = rec)
        return rec
    }

    // 6. Security
    fun toggleVoiceAuth(): Boolean {
        val newState = !_isVoiceAuthEnabled.value
        _isVoiceAuthEnabled.value = newState
        prefs.edit().putBoolean("voice_auth_enabled", newState).apply()
        return newState
    }

    fun togglePrivacyMode(): Boolean {
        val newState = !_isPrivacyMode.value
        _isPrivacyMode.value = newState
        prefs.edit().putBoolean("privacy_mode", newState).apply()
        return newState
    }

    // 7. Offline Models
    fun toggleDownloadModel(id: String) {
        _offlineModels.value = _offlineModels.value.map {
            if (it.id == id) {
                val downloaded = !it.isDownloaded
                it.copy(isDownloaded = downloaded, isInstalled = downloaded)
            } else {
                it
            }
        }
    }

    // 8. Orb Customization
    fun updateOrbCustomization(config: OrbCustomization) {
        _orbCustomization.value = config
        prefs.edit()
            .putFloat("orb_size_scale", config.sizeScale)
            .putFloat("orb_speed_scale", config.speedMultiplier)
            .putString("orb_style", config.style.name)
            .putInt("orb_rings", config.ringCount)
            .putInt("orb_particles", config.particleDensity)
            .putFloat("orb_glow", config.glowIntensity)
            .apply()
    }

    private fun loadOrbCustomization(): OrbCustomization {
        val styleName = prefs.getString("orb_style", OrbStyle.CYBER_CORE.name)
        val style = try {
            OrbStyle.valueOf(styleName ?: OrbStyle.CYBER_CORE.name)
        } catch (e: Exception) {
            OrbStyle.CYBER_CORE
        }
        return OrbCustomization(
            sizeScale = prefs.getFloat("orb_size_scale", 1.0f),
            speedMultiplier = prefs.getFloat("orb_speed_scale", 1.0f),
            style = style,
            ringCount = prefs.getInt("orb_rings", 3),
            particleDensity = prefs.getInt("orb_particles", 2),
            glowIntensity = prefs.getFloat("orb_glow", 1.0f)
        )
    }

    // Plugin toggles
    fun togglePlugin(id: String) {
        _plugins.value = _plugins.value.map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
        }
    }

    private fun initDefaultDevices(): List<SmartDevice> = listOf(
        SmartDevice("1", "Living Room Main Light", "Living Room", "Philips Hue", DeviceType.LIGHT, true, "100% Bright"),
        SmartDevice("2", "Kitchen Ambient Strip", "Kitchen", "LIFX", DeviceType.LIGHT, true, "Warm Amber"),
        SmartDevice("3", "Smart Thermostat", "Hallway", "Google Nest", DeviceType.THERMOSTAT, true, "71°F Auto"),
        SmartDevice("4", "Front Door Deadbolt", "Entrance", "SmartThings", DeviceType.LOCK, true, "Locked"),
        SmartDevice("5", "Bedroom Ceiling Fan", "Bedroom", "TP-Link Kasa", DeviceType.FAN, false, "Speed 2"),
        SmartDevice("6", "Sonos Living Soundbar", "Living Room", "Sonos", DeviceType.SPEAKER, true, "Idle - AirPlay Ready"),
        SmartDevice("7", "Garage Main Door", "Garage", "Meross", DeviceType.GARAGE, false, "Closed & Secured"),
        SmartDevice("8", "Motorized Window Blinds", "Living Room", "HomeKit", DeviceType.BLINDS, false, "Open 100%"),
        SmartDevice("9", "Roborock S8 Pro", "All Rooms", "Xiaomi", DeviceType.VACUUM, false, "Docked • 100% Battery")
    )

    private fun initDefaultModels(): List<OfflineModel> = listOf(
        OfflineModel("gemma2b", "Gemma 2B Local Brain", "Local NLU & Logic", "1.4 GB", true, true),
        OfflineModel("whisper_nano", "Whisper Nano Speech Recognition", "Local Voice Engine", "48 MB", true, true),
        OfflineModel("piper_tts", "Piper Natural Neural TTS", "Offline Human Voice", "65 MB", true, true),
        OfflineModel("vision_edge", "Edge Vision & Screen OCR", "On-Device UI Parsing", "110 MB", false, false)
    )

    private fun initDefaultPlugins(): List<CustomPlugin> = listOf(
        CustomPlugin("spotify_deep", "Spotify Deep Linker & DJ", "Allows seamless voice playback and smart playlist discovery", "MAYA Community", true),
        CustomPlugin("weather_radar", "Hyperlocal Doppler Radar", "Provides real-time storm warnings and 10-day forecasts", "MeteoGroup", true),
        CustomPlugin("crypto_feed", "CoinGecko Real-time Ticker", "Fetches live price alerts for Bitcoin, Ethereum and altcoins", "CoinGecko API", true),
        CustomPlugin("tesla_fleet", "Tesla Fleet API Telemetry", "Controls climate, locks, sentry mode and charge limits", "Tesla Motors", true)
    )

    companion object {
        @Volatile
        private var INSTANCE: UltimateManager? = null

        fun getInstance(context: Context): UltimateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UltimateManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
