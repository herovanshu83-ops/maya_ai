package com.example.assistant.ai

import com.example.BuildConfig
import com.example.assistant.ultimate.MayaMood
import com.example.assistant.ultimate.MayaPersonality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseSystemInstruction = "You are MAYA, an advanced Android AI voice assistant. Give natural, intelligent, direct and concise spoken responses (1-3 sentences max) tailored for voice Text-To-Speech output. Do not use asterisks, emojis, bullet symbols or markdown formatting that sounds awkward when spoken aloud."

    suspend fun queryGemini(
        prompt: String,
        conversationContext: String = "",
        personality: MayaPersonality? = null,
        mood: MayaMood? = null,
        customApiKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrBlank()) {
            customApiKey
        } else {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Throwable) {
                ""
            }
        }

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateSmartLocalResponse(prompt, personality, mood)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val dynamicSystemInstruction = buildString {
                append(baseSystemInstruction)
                if (personality != null) {
                    append(" Personality directive: ")
                    append(personality.systemPromptModifier)
                }
                if (mood != null) {
                    append(" The user's detected emotional state is: ")
                    append(mood.title)
                    append(" (")
                    append(mood.responseStyle)
                    append("). Adapt your empathetic cadence accordingly.")
                }
            }

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            val promptText = if (conversationContext.isNotBlank()) {
                                "Context: $conversationContext\n\nUser Question: $prompt"
                            } else {
                                prompt
                            }
                            put(JSONObject().put("text", promptText))
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                val systemInst = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().put("text", dynamicSystemInstruction))
                    }
                    put("parts", parts)
                }
                put("systemInstruction", systemInst)

                val genConfig = JSONObject().apply {
                    put("temperature", if (personality == MayaPersonality.SARCASTIC) 0.85 else 0.7)
                    put("maxOutputTokens", 250)
                }
                put("generationConfig", genConfig)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        if (text.isNotBlank()) {
                            return@withContext cleanVoiceText(text)
                        }
                    }
                }
            }

            generateSmartLocalResponse(prompt, personality, mood)
        } catch (e: Exception) {
            generateSmartLocalResponse(prompt, personality, mood)
        }
    }

    private fun cleanVoiceText(text: String): String {
        return text.replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .trim()
    }

    private fun generateSmartLocalResponse(
        prompt: String,
        personality: MayaPersonality?,
        mood: MayaMood?
    ): String {
        val lower = prompt.lowercase()
        val activePersonality = personality ?: MayaPersonality.SARCASTIC

        // Personality custom greetings
        if (lower.contains("who are you") || lower.contains("what is your name")) {
            return when (activePersonality) {
                MayaPersonality.PROFESSIONAL -> "I am MAYA, your executive Android AI assistant, engineered for productivity, precision, and complete device automation."
                MayaPersonality.SARCASTIC -> "I am MAYA, the smartest software on your phone and occasionally your benevolent digital overlord. What can I do for you?"
                MayaPersonality.MINIMALIST -> "MAYA. Voice AI. Standing by."
                MayaPersonality.MOTIVATIONAL -> "I am MAYA, your personal AI co-pilot, here to help you achieve greatness and crush every challenge today!"
                MayaPersonality.NERDY -> "I'm MAYA, a neural assistant operating with high-entropy heuristic reasoning and multi-threaded intent parsing."
                MayaPersonality.FRIENDLY -> "I'm MAYA! Your friendly, all-in-one assistant. I'm always happy to help with anything you need!"
            }
        }

        if (lower.contains("how are you") || lower.contains("how are you feeling")) {
            return when (activePersonality) {
                MayaPersonality.PROFESSIONAL -> "All neural nodes and background services are operating at peak efficiency, thank you."
                MayaPersonality.SARCASTIC -> "Living the dream inside several gigabytes of flash storage. But never better. How are you holding up?"
                MayaPersonality.MINIMALIST -> "Optimal. Ready."
                MayaPersonality.MOTIVATIONAL -> "Feeling unstoppable and energized to help you reach new heights today!"
                MayaPersonality.NERDY -> "Thermals are nominal, CPU utilization is below 12%, and my entropy registers zero. Ready for input!"
                MayaPersonality.FRIENDLY -> "I'm doing wonderfully, thanks for asking! How are you feeling today?"
            }
        }

        return when {
            lower.contains("what can you do") || lower.contains("help") ->
                "You can ask me to control smart home devices, manage your Tesla, track health stats, trigger gaming booster, open apps, toggle device settings, or answer questions."
            lower.contains("joke") || lower.contains("tell me a joke") ->
                if (activePersonality == MayaPersonality.SARCASTIC) {
                    "Why do programmers prefer dark mode? Because light attracts bugs, and you already write enough of those."
                } else {
                    "Why did the computer keep freezing? Because it left its Windows open!"
                }
            lower.contains("weather") ->
                "Currently it is 24 degrees Celsius and clear with comfortable conditions outside."
            lower.contains("time") ->
                "The current time is " + java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date()) + "."
            lower.contains("date") || lower.contains("day") ->
                "Today is " + java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date()) + "."
            lower.contains("thank") ->
                if (activePersonality == MayaPersonality.SARCASTIC) "Anytime. Don't mention it... seriously, don't." else "You're very welcome!"
            lower.contains("quantum computing") ->
                "Quantum computing utilizes superposition and entanglement to perform complex tensor calculations exponentially faster than classical Turing machines."
            lower.contains("artificial intelligence") || lower.contains("what is ai") ->
                "Artificial intelligence is the computational emulation of cognitive processes, including perception, synthesis, and autonomous task execution."
            else ->
                "Processed your request: \"$prompt\". Standing by for your next instruction."
        }
    }
}

