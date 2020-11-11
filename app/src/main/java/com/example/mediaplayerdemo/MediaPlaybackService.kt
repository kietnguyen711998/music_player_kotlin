package com.example.mediaplayerdemo

import android.media.AudioManager.STREAM_MUSIC
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver

const val MY_MEDIA_ROOT_ID = "media_root_id"
const val MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id"
const val NOTIFICATION_CHANNEL = "media_notification_channel"
const val NOTIFICATION_ID = 2

private const val TAG = "MediaPlaybackService"

class MediaPlaybackService : MediaBrowserServiceCompat() {

    private var mediaSession: MediaSessionCompat? = null
    private lateinit var stateBuilder: PlaybackStateCompat.Builder

    override fun onCreate() {
        super.onCreate()

        // Create MediaSessionCompat
        mediaSession = MediaSessionCompat(baseContext, "MediaPlaybackService").apply {
            isActive = true
            // Enable callbacks for MediaButtons and TransportControls
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

            // Set initial playback state
            stateBuilder = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE)
            setPlaybackState(stateBuilder.build())
            setPlaybackToLocal(STREAM_MUSIC)
            setCallback(MySessionCallback())

            // Set session's token so that client activities can communicate with it
            setSessionToken(sessionToken)
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(MY_MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {

        if (parentId == MY_EMPTY_MEDIA_ROOT_ID) {
            result.sendResult(null)
            return
        }

        val mediaItems = ArrayList<MediaBrowserCompat.MediaItem>()

        // Check if this is root menu
        if (parentId == MY_MEDIA_ROOT_ID) {
            for (i in 1..10) {
                val mediaItem = MediaBrowserCompat.MediaItem(
                    MediaDescriptionCompat.Builder()
                        .setDescription("Sample Description $i")
                        .setTitle("Sample Media Title $i")
                        .setSubtitle("Sample Media Subtitle $i")
                        .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                )

                mediaItems.add(mediaItem)
            }
        }

        result.sendResult(mediaItems)
    }

    private fun buildNotification() {
        val controller = mediaSession!!.controller
        val mediaMetadata = controller.metadata
        val description = mediaMetadata.description

        val builder = NotificationCompat.Builder(baseContext, NOTIFICATION_CHANNEL).apply {
            setContentTitle(description.title)
            setContentText(description.subtitle)
            setSubText(description.description)
            setLargeIcon(description.iconBitmap)

            setContentIntent(controller.sessionActivity)

            setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    baseContext,
                    PlaybackStateCompat.ACTION_STOP
                )
            )

            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            setSmallIcon(R.drawable.ic_launcher_foreground)
            color = ContextCompat.getColor(applicationContext, R.color.black)

            addAction(
                NotificationCompat.Action(
                    R.drawable.ic_launcher_background,
                    "PAUSE",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        baseContext,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            )

            setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession!!.sessionToken)
                    .setShowActionsInCompactView(0)

                    // Add a cancel button
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            baseContext,
                            PlaybackStateCompat.ACTION_STOP
                        )
                    )
            )
        }

        startForeground(NOTIFICATION_ID, builder.build())
    }

}

class MySessionCallback : MediaSessionCompat.Callback() {
    override fun onPlay() {
        super.onPlay()
        Log.d(TAG, "onPlay: ")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: ")
    }
}
