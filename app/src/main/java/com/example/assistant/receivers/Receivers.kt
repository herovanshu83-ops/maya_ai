package com.example.assistant.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val CHANNEL_ID = "maya_alarms_channel"
        private const val NOTIFICATION_ID = 2001
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        Toast.makeText(context, "⏰ Alarm ringing!", Toast.LENGTH_LONG).show()

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "MAYA Alarms",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "MAYA Alarm Alerts"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("⏰ Alarm Ringing")
                .setContentText("MAYA Assistant scheduled alarm trigger")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(alarmSound)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class TimerReceiver : BroadcastReceiver() {
    companion object {
        private const val CHANNEL_ID = "maya_timers_channel"
        private const val NOTIFICATION_ID = 2002
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        Toast.makeText(context, "⏱️ Timer finished!", Toast.LENGTH_LONG).show()

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "MAYA Timers",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "MAYA Timer Alerts"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("⏱️ Timer Finished")
                .setContentText("Your MAYA timer has elapsed!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(notificationSound)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
