package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entities.CommandHistory
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.RoutineEntity
import com.example.data.local.entities.SettingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AssistantRepository(private val database: AppDatabase) {

    val recentCommands: Flow<List<CommandHistory>> = database.commandHistoryDao().getRecentHistory(100)
    val allMemories: Flow<List<MemoryEntity>> = database.memoryDao().getAllMemories()
    val allRoutines: Flow<List<RoutineEntity>> = database.routineDao().getAllRoutines()
    val allSettings: Flow<List<SettingEntity>> = database.settingDao().getAllSettings()

    suspend fun logCommand(text: String, intentType: String, response: String, isSuccess: Boolean = true) {
        database.commandHistoryDao().insertCommand(
            CommandHistory(
                commandText = text,
                intentType = intentType,
                responseText = response,
                isSuccess = isSuccess
            )
        )
    }

    suspend fun clearHistory() {
        database.commandHistoryDao().clearAll()
    }

    suspend fun saveMemory(key: String, value: String, category: String = "personal") {
        database.memoryDao().insertMemory(
            MemoryEntity(
                key = key.trim(),
                value = value.trim(),
                category = category
            )
        )
    }

    suspend fun findMemory(key: String): MemoryEntity? {
        return database.memoryDao().getByKey(key.trim())
    }

    suspend fun deleteMemory(id: Long) {
        database.memoryDao().deleteById(id)
    }

    suspend fun clearAllMemories() {
        database.memoryDao().clearAll()
    }

    suspend fun saveRoutine(name: String, triggerPhrase: String, actions: List<String>) {
        database.routineDao().insertRoutine(
            RoutineEntity(
                name = name,
                triggerPhrase = triggerPhrase,
                actionsJson = actions.joinToString(";")
            )
        )
    }

    suspend fun findRoutine(nameOrTrigger: String): RoutineEntity? {
        return database.routineDao().findRoutine(nameOrTrigger, nameOrTrigger)
    }

    suspend fun updateRoutine(routine: RoutineEntity) {
        database.routineDao().updateRoutine(routine)
    }

    suspend fun deleteRoutine(id: Long) {
        database.routineDao().deleteById(id)
    }

    suspend fun getSetting(key: String, default: String): String {
        return database.settingDao().getValue(key) ?: default
    }

    suspend fun setSetting(key: String, value: String) {
        database.settingDao().setSetting(SettingEntity(key, value))
    }

    suspend fun seedDefaultRoutinesIfEmpty() {
        val current = allRoutines.firstOrNull() ?: emptyList()
        if (current.isEmpty()) {
            saveRoutine(
                name = "Good Morning",
                triggerPhrase = "good morning",
                actions = listOf("Turn on flashlight", "Set volume to 50%", "Check battery status", "Weather forecast")
            )
            saveRoutine(
                name = "Work Mode",
                triggerPhrase = "work mode",
                actions = listOf("Set volume to 20%", "Open Calendar", "Open Gmail")
            )
            saveRoutine(
                name = "Bedtime",
                triggerPhrase = "good night",
                actions = listOf("Set volume to 0%", "Set alarm for 7 AM", "Turn off flashlight")
            )
        }
    }
}
