package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.assistant.services.MayaForegroundService
import com.example.data.local.AppDatabase
import com.example.data.repository.AssistantRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MayaApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: AssistantRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = AssistantRepository(database)

        createNotificationChannels()

        applicationScope.launch {
            repository.seedDefaultRoutinesIfEmpty()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MayaForegroundService.CHANNEL_ID,
                "MAYA Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background voice monitoring and assistant status"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
