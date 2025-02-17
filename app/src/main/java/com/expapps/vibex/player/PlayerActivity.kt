package com.expapps.vibex.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.expapps.vibex.Constants
import com.expapps.vibex.R
import com.expapps.vibex.Utils
import com.expapps.vibex.adapters.SongListAdapter
import com.expapps.vibex.databinding.ActivityPlayerBinding
import com.expapps.vibex.listeners.MusicClickListener
import com.expapps.vibex.models.SongsModel
import com.google.firebase.storage.internal.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private var mediaPlayerService: MediaPlayerService? = null
    private var isBound = false

    private lateinit var adapter: SongListAdapter

    private var isGotSongUpdate = false

    private val mediaPlayerServiceIntent by lazy {
        Intent(this, MediaPlayerService::class.java)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MediaPlayerService.MediaPlayerBinder
            mediaPlayerService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaPlayerService = null
            isBound = false
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = SongListAdapter(isFromRecommended = true)

        adapter.musicClickListener = MusicClickListener { filePath, songUrl, songTitle, artistName, songsModel ->
            playSong(songsModel)
        }

        val serviceIntent = Intent(this, MediaPlayerService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        updateUI()
        setButtonListeners()

        binding.recommendedSongsRecycler.layoutManager = LinearLayoutManager(this)
        binding.recommendedSongsRecycler.adapter = adapter

        binding.backIv.setOnClickListener {
            finish()
        }

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                // Update progress again
                updateUI()
                // Repeat after a certain delay (e.g., 1 second)
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        if (isBound) {
            val songInfo = mediaPlayerService?.getCurrentSongInfo()
            if (!isGotSongUpdate) {
                isGotSongUpdate = true
                if (songInfo != null) {
                    recommendSongs(songInfo)
                }
            }
            if (songInfo?.isPrepared == true) {
                binding.progressCircle.visibility = View.GONE
                binding.playPauseBtn.visibility = View.VISIBLE
            } else {
                binding.progressCircle.visibility = View.VISIBLE
                binding.playPauseBtn.visibility = View.GONE
            }
            binding.seekbarProgress.max = songInfo?.totalDuration?.toIntOrNull() ?: 0
            // Update UI with songInfo
            binding.seekbarProgress.progress = songInfo?.currentDuration?.toIntOrNull() ?: 0
            binding.textTitle.text = songInfo?.songTitle
            binding.textArtist.text = songInfo?.artist
            binding.currentDurationTv.text = Utils.formatTimeStamp((songInfo?.currentDuration?.toIntOrNull() ?: 0))
            binding.totalDurationTv.text = Utils.formatTimeStamp((songInfo?.totalDuration?.toIntOrNull() ?: 0))
            updatePlayPauseButton()
        }
    }

    private fun updatePlayPauseButton() {
        if (mediaPlayerService?.isPlaying() == true) {
            binding.playPauseBtn.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pause))
        } else {
            binding.playPauseBtn.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_play))
        }
        mediaPlayerService?.updateNotificationPlayPauseIcon()
    }

    private fun recommendSongs(songInfo: SongInfo) {
        val recommendedSongs = MusicUtils.addSongsToQueue(songInfo.songTitle ?: "", songInfo.artist ?: "NONE", songInfo.genreTags ?: "NONE", MusicUtils.allSongs)
        adapter.setData(recommendedSongs)
    }


    private fun playSong(songsModel: SongsModel) {
        mediaPlayerServiceIntent.action = Constants.ACTION_PLAY_SONG
        mediaPlayerServiceIntent.putExtra(Constants.EXTRA_SONG_URL, if (songsModel.isLocalSong) songsModel.localSongPath else songsModel.songUrl)
        mediaPlayerServiceIntent.putExtra(Constants.EXTRA_SONG_TITLE, songsModel.songTitle)
        mediaPlayerServiceIntent.putExtra(Constants.EXTRA_SONG_GENRE, songsModel.songGenre)
        mediaPlayerServiceIntent.putExtra(Constants.EXTRA_SONG_ARTIST, songsModel.songArtists)
        ContextCompat.startForegroundService(this, mediaPlayerServiceIntent)
    }


    private fun setButtonListeners() {
        binding.playPauseBtn.setOnClickListener {
            mediaPlayerService?.playPauseSong()
            updatePlayPauseButton()
        }
        binding.previousBtn.setOnClickListener {
            if (adapter.getCurrentPlayingPosition() > 0) {
                binding.recommendedSongsRecycler.findViewHolderForAdapterPosition(adapter.getCurrentPlayingPosition() - 1)?.itemView?.performClick()
            }
        }
        binding.nextBtn.setOnClickListener {
            if (adapter.getCurrentPlayingPosition() < 0) {
                binding.recommendedSongsRecycler.findViewHolderForAdapterPosition(0)?.itemView?.performClick()
            } else if (adapter.getCurrentPlayingPosition() < adapter.itemCount) {
                binding.recommendedSongsRecycler.findViewHolderForAdapterPosition(adapter.getCurrentPlayingPosition() + 1)?.itemView?.performClick()
            }
        }

        binding.seekbarProgress.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayerService?.seekTo(position = progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        isGotSongUpdate = false
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}