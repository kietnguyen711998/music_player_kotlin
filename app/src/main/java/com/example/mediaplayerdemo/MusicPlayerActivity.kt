package com.example.mediaplayerdemo

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Icon
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.PersistableBundle
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.mediaplayerdemo.MainActivity.Companion.POSITION

private const val TAG = "MusicPlayerActivity"

class MusicPlayerActivity : AppCompatActivity(), ActionInterface {

    private lateinit var albumArt: ImageView
    private lateinit var songName: TextView
    private lateinit var songArtist: TextView
    private lateinit var shuffle: ImageView
    private lateinit var prev: ImageView
    private lateinit var play: ImageView
    private lateinit var next: ImageView
    private lateinit var repeat: ImageView
    private lateinit var currentDuration: TextView
    private lateinit var totalDuration: TextView
    private lateinit var seekBar: SeekBar
    private val handler = Handler()

    private var isRepeatMode = false

    private var position = -1
    private var musicFile: MusicFile? = null
    private var mediaSession: MediaSessionCompat? = null
    private var isBinded = false

    private var mService: MusicService? = null

    val actionListener = object : ActionInterface {
        override fun playPause() {
            this@MusicPlayerActivity.playPause()
        }

        override fun playNext() {
            this@MusicPlayerActivity.playNext()
        }

        override fun playPrev() {
            this@MusicPlayerActivity.playPrev()
        }

    }

    private val serviceConnectionCallback = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.LocalBinder
            Log.d(TAG, "onServiceConnected: ")
            mService = binder.getService(actionListener)
            if (!isBinded) {
                setupMediaPlayer()
                isBinded = true
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected: ")
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)

        Log.d(TAG, "onCreate: ${savedInstanceState?.getInt(POSITION)}")
        mediaSession = MediaSessionCompat(baseContext, "Media Player Mine")
        initViews()
        getIntentData()
    }

    override fun onStart() {
        super.onStart()
        Intent(this, MusicService::class.java).apply {
            bindService(this, serviceConnectionCallback, BIND_AUTO_CREATE)
        }

        play.setOnClickListener { playPause() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (seekBar != null && fromUser) {
                    mService?.seekTo(progress * 1000)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        next.setOnClickListener { playNext() }

        prev.setOnClickListener { playPrev() }

        repeat.setOnClickListener {
            isRepeatMode = isRepeatMode.not()
            if (isRepeatMode) {
                notifyUser("Repeat on")
            } else {
                notifyUser("Repeat off")

            }
            mediaPlayerSettingChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
        unbindService(serviceConnectionCallback)
    }

    private fun notifyUser(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    }

    override fun playPrev() {
        MainActivity.musicFilesFiltered.size.apply {
            position = if ((position - 1) < 0) {
                this - 1
            } else {
                position + 1
            }
        }
        updateSong()
    }

    override fun playNext() {
        MainActivity.musicFilesFiltered.size.apply {
            position = if ((position + 1) > this) {
                1
            } else {
                (position + 1) % this
            }
        }
        updateSong()
    }

    private fun updateSong() {
        musicFile = MainActivity.musicFilesFiltered[position]
        setupMediaPlayer()
        setUpUI()
    }

    override fun playPause() {
        if (mService!!.isPlaying()) {
            mService?.pause()
            play.setImageResource(R.drawable.icon_play)
        } else {
            mService?.start()
            play.setImageResource(R.drawable.icon_pause)
        }
    }

    private fun setupMediaPlayer() {
        Log.d(TAG, "setupMediaPlayer: $position $musicFile")
        play.setImageResource(R.drawable.icon_pause)
        mService?.createMediaPlayer(musicFile!!.uri, position)
        mService?.player?.setOnCompletionListener {
            mediaPlayerSettingChanged()
            if (isRepeatMode) {
                setupMediaPlayer()
            } else {
                playNext()
            }
        }
        mService?.start()

        val duration = mService!!.duration() / 1000
        seekBar.max = duration

        totalDuration.text = formattedTime(duration)

        runOnUiThread(object : Runnable {
            override fun run() {
                // Without try/catch block app crashes because of @IllegalStateException
                try {
                    if (mService!!.isPlaying()) {
                        val currentPosition = mService!!.currentPosition() / 1000
                        seekBar.progress = currentPosition
                        currentDuration.text = formattedTime(currentPosition)
                        handler.postDelayed(this, 1000)
                    }
                } catch (e: IllegalStateException) {
                    handler.removeCallbacksAndMessages(null)
                }

            }
        })
    }

    private fun mediaPlayerSettingChanged() {
        if (isRepeatMode) {
            repeat.setImageResource(R.drawable.icon_repeat_one)
        } else {
            repeat.setImageResource(R.drawable.icon_repeat)
        }
    }

    private fun formattedTime(duration: Int): String? {
        val minutes = (duration / 60).toString()
        val seconds = (duration % 60).toString()

        return when (seconds.length == 2) {
            true -> "0$minutes:$seconds"
            else -> "0$minutes:0$seconds"
        }
    }

    private fun getIntentData() {
        val extras = intent.extras

        if (extras != null) {
            position = extras.getInt(POSITION, -1)
            musicFile = MainActivity.musicFilesFiltered[position]
            setUpUI()
        }

    }

    private fun setUpUI() {
        songName.animate().alpha(0f).setDuration(2000).start()
        songArtist.animate().alpha(0f).setDuration(2000).start()

        songName.text = musicFile?.title
        songArtist.text = musicFile?.artist

        songName.animate().alpha(1f).setDuration(2000).setStartDelay(2000).start()
        songArtist.animate().alpha(1f).setDuration(2000).setStartDelay(2000).start()
    }

    private fun initViews() {
        albumArt = findViewById(R.id.albumArt)
        songName = findViewById(R.id.songName)
        songArtist = findViewById(R.id.songArtist)
        shuffle = findViewById(R.id.shuffle)
        prev = findViewById(R.id.prev)
        play = findViewById(R.id.play)
        next = findViewById(R.id.next)
        repeat = findViewById(R.id.repeat)
        currentDuration = findViewById(R.id.currentDuration)
        totalDuration = findViewById(R.id.totalDuration)
        seekBar = findViewById(R.id.seekBar)
    }

}