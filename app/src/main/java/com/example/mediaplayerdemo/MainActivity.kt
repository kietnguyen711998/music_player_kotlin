package com.example.mediaplayerdemo

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import java.util.*
import kotlin.collections.ArrayList

private const val TAG = "MainActivity"
private const val READ_EXTERNAL_PERMISSION = 5

class MainActivity : AppCompatActivity() {

    companion object {
        val musicFiles = arrayListOf<MusicFile>()
        val musicFilesFiltered = arrayListOf<MusicFile>()
        const val POSITION = "Song_Position"
    }

    private lateinit var mediaBrowserCompat: MediaBrowserCompat

    private lateinit var recyclerView: RecyclerView
    private val musicAdapter = MusicAdapter(musicFilesFiltered)

    //    private lateinit var playPauseButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.musicList)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = musicAdapter

        mediaBrowserCompat = MediaBrowserCompat(
            this,
            ComponentName(this, MediaPlaybackService::class.java),
            connectionCallback,
            null
        )
        requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_PERMISSION)
    }

    override fun onStart() {
        super.onStart()
        mediaBrowserCompat.connect()

    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onStop() {
        super.onStop()
        MediaControllerCompat.getMediaController(this).unregisterCallback(controllerCallback)
        mediaBrowserCompat.disconnect()
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            Log.d(TAG, "onPlaybackStateChanged: $state")
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            Log.d(TAG, "onMetadataChanged: $metadata")
        }
    }

    private val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            super.onConnected()

            // Get the token from mediaSession
            mediaBrowserCompat.sessionToken.also { token ->

                // Create Media Controller
                val mediaController = MediaControllerCompat(this@MainActivity, token)

                // save the token
                MediaControllerCompat.setMediaController(this@MainActivity, mediaController)

                Log.d(TAG, "onConnected: ")
                buildTransportControls()
            }
        }

        override fun onConnectionSuspended() {
            // The Service has crashed. Disable transport controls until it automatically reconnects
        }

        override fun onConnectionFailed() {
            // The Service has refused our connection
        }
    }

    private fun buildTransportControls() {
        val mediaController = MediaControllerCompat.getMediaController(this@MainActivity)

        val pbState = mediaController.playbackState.state

        Log.d(TAG, "buildTransportControls: ${mediaController.playbackState}")
        Log.d(TAG, "buildTransportControls: ${mediaController.metadata}")

        mediaController.registerCallback(controllerCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        val searchItem = menu.findItem(R.id.search)
        val searchView = searchItem.actionView as SearchView

        searchView.imeOptions = EditorInfo.IME_ACTION_DONE

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                musicAdapter.filter.filter(newText)
                musicAdapter.notifyDataSetChanged()
                return false
            }

        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.byName -> {
                onSort(item.title.toString())
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun getAllSongs(context: Context) {
        val url = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA
        )

        context.contentResolver.query(url, projection, null, null, null).also { cursor ->
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val title = cursor.getString(0)
                    val artist = cursor.getString(1)
                    val album = cursor.getString(2)
                    val uri = cursor.getString(3)

                    val musicFile = MusicFile(uri, title, artist, album)

                    getAlbumArt(uri, title)
                    Log.d(TAG, "getAllSongs: $musicFile")
                    musicFiles.add(musicFile)
                    musicFilesFiltered.add(musicFile)
                }
            }
            cursor?.close()
        }
    }

    private fun getAlbumArt(uri: String, title: String) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(uri)
        Log.d(TAG, "getAlbumArt: ${retriever.embeddedPicture}")
        retriever.release()
    }

    private fun requestPermission(perm: String, code: Int) {
        val permission = ContextCompat.checkSelfPermission(this, perm)

        if (permission == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(perm), code)
        } else {
            getAllSongs(this@MainActivity)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        when (requestCode) {
            READ_EXTERNAL_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getAllSongs(this@MainActivity)
                }
            }
        }
    }

    private inner class MusicHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var albumArt: ImageView = itemView.findViewById(R.id.albumArt)
        private var musicTitle: TextView = itemView.findViewById(R.id.musicTitle)
        private var menu: ImageView = itemView.findViewById(R.id.menu)

        fun bind(musicFile: MusicFile, position: Int) {
            menu.setOnClickListener {
                Log.d(TAG, "bind: setOnClickListener")
                val popupMenu = PopupMenu(this@MainActivity, menu)
                popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
                popupMenu.show()

                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.delete_item -> {
                            deleteSong(position, itemView)
                            true
                        }
                        else -> true
                    }

                }

            }
            itemView.setOnClickListener {
                val intent = Intent(this@MainActivity, MusicPlayerActivity::class.java).apply {
                    putExtra(POSITION, position)
                }
                startActivity(intent)
                mediaController.transportControls.play()
            }

            albumArt.setImageResource(R.mipmap.album1)
            musicTitle.text = musicFile.title
        }

    }

    private fun deleteSong(position: Int, itemView: View) {
        val temp = musicFilesFiltered[position]
        musicFilesFiltered.removeAt(position)
        musicAdapter.notifyItemRemoved(position)
        musicAdapter.notifyItemRangeChanged(position, musicFilesFiltered.size)
        Snackbar.make(itemView, "Undo deletion of ${temp.title}", Snackbar.LENGTH_LONG)
            .setAction(
                "UNDO"
            ) {
                musicFilesFiltered.add(position, temp)
                musicAdapter.notifyItemInserted(position)
                musicAdapter.notifyItemRangeChanged(position, musicFilesFiltered.size)
            }
            .setActionTextColor(
                ResourcesCompat.getColor(
                    resources,
                    android.R.color.holo_blue_dark,
                    null
                )
            )
            .show()

    }

    val searchFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList = mutableListOf<MusicFile>()

            if (constraint.isNullOrEmpty()) {
                filteredList.addAll(musicFiles)
            } else {
                val query = constraint.toString().toLowerCase(Locale.ROOT).trim()

                musicFiles.map {
                    if (it.title.toLowerCase(Locale.ROOT).contains(query) || it.artist.toLowerCase(
                            Locale.ROOT
                        ).contains(query)
                    ) {
                        filteredList.add(it)
                    }
                }

            }
            return FilterResults().apply {
                values = filteredList
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            val songs = results!!.values as MutableList<MusicFile>
            musicFilesFiltered.clear()
            musicFilesFiltered.addAll(songs)
        }
    }


    private inner class MusicAdapter(private val musics: ArrayList<MusicFile>) :
        RecyclerView.Adapter<MusicHolder>(), Filterable {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicHolder {
            return MusicHolder(layoutInflater.inflate(R.layout.music_list_item, parent, false))
        }

        override fun onBindViewHolder(holder: MusicHolder, position: Int) {
            holder.bind(musics[position], position)
        }

        override fun getItemCount() = musics.size

        override fun getFilter() = searchFilter
    }

    private fun onSort(orderBy: String) {
        Collections.sort(musicFilesFiltered, Comparator { o1, o2 -> o1.title.compareTo(o2.title) })
        musicAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        val intent = Intent(this, MusicService::class.java)
        stopService(intent)
    }
}