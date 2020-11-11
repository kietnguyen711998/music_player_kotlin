package com.example.mediaplayerdemo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

const val CHANNEL_ID = "Channel_id"

class MusicApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(
            CHANNEL_ID,
            "This is notification title",
            "This is notification description"
        )
    }

    private fun createNotificationChannel(channelId: String, title: String, description: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, title, NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = description
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

    }
}