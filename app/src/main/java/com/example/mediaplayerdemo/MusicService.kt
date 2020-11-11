package com.example.mediaplayerdemo

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat

private const val TAG = "MusicService"

class MusicService() : Service() {
    private lateinit var uri: Uri
    private var position = -1
    private var mediaSession: MediaSessionCompat? = null
    private var actionListener: ActionInterface? = null

    companion object {
        private var mediaPlayer: MediaPlayer? = null
    }

    val player
        get() = mediaPlayer

    private val mBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(baseContext, "Media Player Mine")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_PREV -> {
                    actionListener?.playPrev()
                }
                ACTION_PLAY -> {
                    actionListener?.playPause()
                }
                ACTION_NEXT -> {
                    actionListener?.playNext()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): Binder {
        Log.d(TAG, "onBind: ");
        return mBinder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
        pause()
        stop()
        release()
    }
    inner class LocalBinder() : Binder() {
        fun getService(listener: ActionInterface): MusicService {
            if (actionListener == null) {
                actionListener = listener
            }
            return this@MusicService
        }
    }

    fun createMediaPlayer(uri: String, pos: Int) {
        position = pos
        startForeground(3, showNotification(R.drawable.icon_pause))
        Log.d(TAG, "createMediaPlayer: $mediaPlayer ${mediaPlayer?.isPlaying}")
        if (mediaPlayer != null) {
            Log.d(TAG, "createMediaPlayer: ")
            stop()
            release()
        }
        this.uri = Uri.parse(uri)
        mediaPlayer = MediaPlayer.create(baseContext, this.uri)
    }

    fun seekTo(i: Int) {
        mediaPlayer?.seekTo(i)
    }

    fun start() {
        mediaPlayer?.start()
        startForeground(3, showNotification(R.drawable.icon_pause))
    }

    fun pause() {
        mediaPlayer?.pause()
        startForeground(3, showNotification(R.drawable.icon_play))
    }

    private fun stop() {
        mediaPlayer?.stop()
    }

    private fun release() {
        mediaPlayer?.release()
    }

    fun isPlaying() = mediaPlayer?.isPlaying ?: false

    fun duration() = mediaPlayer?.duration ?: 0

    fun currentPosition() = mediaPlayer?.currentPosition ?: 0

    private fun showNotification(playPauseIcon: Int): Notification {
        var intent = Intent(this, NotificationReceiver::class.java).apply {
            action = ACTION_PREV
        }
        val prevPendingIntent =
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        intent = Intent(this, NotificationReceiver::class.java).apply {
            action = ACTION_NEXT
        }
        val nextPendingIntent =
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        intent = Intent(this, NotificationReceiver::class.java).apply {
            action = ACTION_PLAY
        }
        val playPausePendingIntent =
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        intent = Intent(this, MusicPlayerActivity::class.java)
        val notificationIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(baseContext, CHANNEL_ID)
            .setContentTitle(MainActivity.musicFiles[position].title)
            .setContentText(MainActivity.musicFiles[position].album)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(notificationIntent)
            .setSmallIcon(R.drawable.icon_shuffle)
            .addAction(
                R.drawable.icon_prev,
                "Prev",
                prevPendingIntent
            )
            .addAction(
                playPauseIcon,
                "Pause",
                playPausePendingIntent
            ).addAction(
                R.drawable.icon_next,
                "Next",
                nextPendingIntent
            )

            .setOnlyAlertOnce(true)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
            )
            .build()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind: ")
        return super.onUnbind(intent)
    }
}