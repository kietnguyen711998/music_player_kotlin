package com.example.mediaplayerdemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.mediaplayerdemo.MainActivity.Companion.POSITION

private const val TAG = "NotificationReceiver"

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && intent.action != null) {
            val serviceIntent = Intent(context, MusicService::class.java)
            when (intent.action) {
                ACTION_PREV -> {
                    serviceIntent.action = ACTION_PREV
                }
                ACTION_PLAY -> {
                    serviceIntent.action = ACTION_PLAY
                }
                ACTION_NEXT -> {
                    serviceIntent.action = ACTION_NEXT
                }
            }
            context?.startService(serviceIntent)
        }
    }

}