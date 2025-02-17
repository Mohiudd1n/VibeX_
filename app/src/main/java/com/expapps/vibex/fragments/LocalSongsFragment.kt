package com.expapps.vibex.fragments

import android.app.ProgressDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.expapps.vibex.Constants
import com.expapps.vibex.adapters.SongListAdapter
import com.expapps.vibex.databinding.FragmentLocalSongsBinding
import com.expapps.vibex.listeners.MusicClickListener
import com.expapps.vibex.models.SongsModel
import com.expapps.vibex.player.AudioPlayerService
import com.expapps.vibex.player.MediaPlayerService
import com.expapps.vibex.player.MusicUtils
import com.expapps.vibex.player.PlayerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class LocalSongsFragment : Fragment() {

    private lateinit var binding: FragmentLocalSongsBinding
    private lateinit var adapter: SongListAdapter
    private var mediaPlayerService: MediaPlayerService? = null

    private val mediaPlayerServiceIntent by lazy {
        Intent(requireContext(), MediaPlayerService::class.java)
    }

    private val audioPlayerService by lazy {
        Intent(requireContext(), AudioPlayerService::class.java)
    }

    private lateinit var progressDialog: ProgressDialog


    private var isBound = false
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
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLocalSongsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initProgressDialog()

        val musicUtils = MusicUtils()
        adapter = SongListAdapter(isFromLocal = true)

        val serviceIntent = Intent(requireContext(), MediaPlayerService::class.java)
        requireContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        adapter.musicClickListener = MusicClickListener { filePath, songUrl, songTitle, artistName, songsModel ->
            playSong(songsModel)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter


        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                progressDialog.show()
            }

            musicUtils.getAudioFiles()
        }

        musicUtils.liveData.observe(requireActivity()) {
            Handler().postDelayed({
                MusicUtils.addAllUniqueSongs(it)
            }, 500)

            progressDialog.dismiss()
            adapter.setData(it)
        }
    }


    private fun playSong(songsModel: SongsModel) {
        mediaPlayerServiceIntent.action = Constants.ACTION_PLAY_SONG
        mediaPlayerServiceIntent.putExtra(Constants.EXTRA_SONG_URL, songsModel.localSongPath)
        mediaPlayerServiceIntent.putExtra(Constants.EXTRA_SONG_TITLE, songsModel.songTitle)
        mediaPlayerServiceIntent.putExtra(Constants.EXTRA_SONG_GENRE, songsModel.songGenre)
        mediaPlayerServiceIntent.putExtra(Constants.EXTRA_SONG_ARTIST, songsModel.songArtists)
        ContextCompat.startForegroundService(requireContext(), mediaPlayerServiceIntent)

        startActivity(Intent(requireContext(), PlayerActivity::class.java))
    }


    private fun initProgressDialog() {
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setTitle("VibeX")
        progressDialog.setMessage("Loading songs...")
        //progressDialog.setCancelable(false)
        //progressDialog.setCanceledOnTouchOutside(false)
    }


    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            requireContext().unbindService(serviceConnection)
            isBound = false
        }
    }


    private fun playSong2(url: String) {
        audioPlayerService.putExtra("stream_url", url)
        requireActivity().startService(audioPlayerService)
    }


}