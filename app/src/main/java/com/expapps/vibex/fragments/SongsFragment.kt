package com.expapps.vibex.fragments

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.expapps.vibex.Constants
import com.expapps.vibex.FirebaseSource
import com.expapps.vibex.R
import com.expapps.vibex.adapters.SongListAdapter
import com.expapps.vibex.databinding.FragmentSongsBinding
import com.expapps.vibex.listeners.MusicClickListener
import com.expapps.vibex.models.SongsModel
import com.expapps.vibex.player.AudioPlayerService
import com.expapps.vibex.player.MediaPlayerService
import com.expapps.vibex.player.MusicUtils
import com.expapps.vibex.player.PlayerActivity


class SongsFragment : Fragment() {

    private lateinit var binding: FragmentSongsBinding
    private lateinit var songAdapter: SongListAdapter

    private lateinit var firebaseSource: FirebaseSource

    private lateinit var progressDialog: ProgressDialog

    private val mediaPlayerServiceIntent by lazy {
        Intent(requireContext(), MediaPlayerService::class.java)
    }

    private val audioPlayerService by lazy {
        Intent(requireContext(), AudioPlayerService::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSongsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initProgressDialog()

        firebaseSource = FirebaseSource()

        songAdapter = SongListAdapter()

        songAdapter.musicClickListener = MusicClickListener { filePath, songUrl, songTitle, artistName, songsModel ->
            if (songUrl.isNotBlank()) {
                playSong(songsModel)
            }
        }

        binding.songsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.songsRecyclerView.adapter = songAdapter

        getAllSongs()

    }

    private fun initProgressDialog() {
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setTitle("VibeX")
        progressDialog.setMessage("Loading songs...")
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
    }

    private fun getAllSongs() {
        progressDialog.show()
        firebaseSource.getAllSongs().observe(requireActivity()) { songsArray ->
            if (songsArray != null) {
                MusicUtils.addAllUniqueSongs(songsArray)
            }
            if (songsArray != null && songsArray.isNotEmpty()) {
                songAdapter.setData(songsArray)
            }
            progressDialog.dismiss()
        }
    }



    private fun playSong(songsModel: SongsModel) {
        mediaPlayerServiceIntent.action = Constants.ACTION_PLAY_SONG
        mediaPlayerServiceIntent.putExtra(Constants.EXTRA_SONG_URL, songsModel.songUrl)
        mediaPlayerServiceIntent.putExtra(Constants.EXTRA_SONG_TITLE, songsModel.songTitle)
        mediaPlayerServiceIntent.putExtra(Constants.EXTRA_SONG_GENRE, songsModel.songGenre)
        mediaPlayerServiceIntent.putExtra(Constants.EXTRA_SONG_ARTIST, songsModel.songArtists)

        ContextCompat.startForegroundService(requireContext(), mediaPlayerServiceIntent)

        startActivity(Intent(requireContext(), PlayerActivity::class.java))
    }

    private fun playSong2(url: String) {
        audioPlayerService.putExtra("stream_url", url)
        requireActivity().startService(audioPlayerService)
        //ContextCompat.startForegroundService(requireContext(), audioPlayerService)
    }

}