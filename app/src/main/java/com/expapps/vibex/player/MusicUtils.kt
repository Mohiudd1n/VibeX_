package com.expapps.vibex.player

import android.media.MediaMetadataRetriever
import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.expapps.vibex.models.SongsModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class MusicUtils {
    val retriever = MediaMetadataRetriever()

    val minimumFetchedSongs = ArrayList<SongsModel>()

    private val mutableLiveData = MutableLiveData<ArrayList<SongsModel>>()
    val liveData: LiveData<ArrayList<SongsModel>> = mutableLiveData

    private var isFirstLoad = true

    companion object {
        var songsPerFetch = 10
        var quickAccessAudioFiles = ArrayList<SongsModel>()
        var allSongs = ArrayList<SongsModel>()
        var songsQueue = ArrayList<SongsModel>()

        var isRunningLoop = false

        fun addAllUniqueSongs(songs: ArrayList<SongsModel>) {
            if (!isRunningLoop) {
                isRunningLoop = true
                try {
                    songs.forEach {
                        if (!allSongs.contains(it)) {
                            allSongs.add(it)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                isRunningLoop = false
            }
        }

        fun addSongsToQueue(title: String, artists: String, genreTags: String, songsList: ArrayList<SongsModel>): ArrayList<SongsModel> {
            songsQueue.clear()
            if (artists != "" && artists.lowercase() != "none") {
                songsList.forEach {
                    if (it.songArtists != null && it.songArtists != "" && it.songArtists?.lowercase() != "none") {
                        if (it.songArtists?.lowercase()?.contains(artists.lowercase()) == true) {
                            if (it.songTitle?.lowercase() != title.lowercase()) {
                                if (!songsQueue.contains(it)) {
                                    songsQueue.add(it)
                                }
                            }
                        }
                    }
                }
            }
            if (genreTags.lowercase() != "" && genreTags.lowercase() != "none") {
                songsList.forEach {
                    if (it.songGenre?.lowercase() != null && it.songGenre?.lowercase() != "none" && it.songGenre?.lowercase() != "") {
                        if (it.songGenre?.lowercase()?.contains(genreTags.lowercase()) == true) {
                            if (it.songTitle?.lowercase() != title.lowercase()) {
                                if (!songsQueue.contains(it)) {
                                    songsQueue.add(it)
                                }
                            }
                        }
                    }
                }
            }
            if (songsQueue.isEmpty()) {
                var count = 0
                val shuffledSongs = songsList.shuffled()
                shuffledSongs.forEach {
                    if (count == 3) {
                        return@forEach
                    }
                    if (it.songTitle?.lowercase() != title.lowercase()) {
                        if (!songsQueue.contains(it)) {
                            songsQueue.add(it)
                        }
                    }
                    count++
                }
            }
            return songsQueue
        }
    }

    private suspend fun getAllAudioFiles(): ArrayList<SongsModel> {
        val audioFiles = ArrayList<SongsModel>()
        val rootDirectory = Environment.getExternalStorageDirectory()
        searchForAudioFiles(rootDirectory, audioFiles)
        if (audioFiles.isNotEmpty()) {
            quickAccessAudioFiles.addAll(audioFiles)
        }
        return audioFiles
    }

    suspend fun getAudioFiles(): ArrayList<SongsModel> {
        if (quickAccessAudioFiles.isNotEmpty()) {
            mutableLiveData.postValue(quickAccessAudioFiles)
            return quickAccessAudioFiles
        }
        return getAllAudioFiles()
    }

    private suspend fun searchForAudioFiles(directory: File, audioFiles: ArrayList<SongsModel>) {
        val files = directory.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    // Recursively search directories
                    searchForAudioFiles(file, audioFiles)
                } else {
                    // Check if the file is an audio file
                    val filePath = file.absolutePath
                    if (isAudioFile(filePath)) {
                        withContext(Dispatchers.Main) {
                            val fileName = file.name
                            try {
                                retriever.setDataSource(filePath)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                            audioFiles.add(SongsModel(songTitle = fileName, localSongPath = filePath, songArtists = artist, songGenre = genre, isLocalSong = true))

                            minimumFetchedSongs.add(SongsModel(songTitle = fileName, localSongPath = filePath, songArtists = artist, songGenre = genre, isLocalSong = true))
                            if (audioFiles.size >= songsPerFetch) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    if (!isFirstLoad) {
                                        delay(200)
                                    }
                                    mutableLiveData.postValue(audioFiles)
                                    isFirstLoad = false
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    private fun isAudioFile(filePath: String): Boolean {
        // Add more audio file extensions as needed
        return filePath.endsWith(".mp3") || filePath.endsWith(".m4a") || filePath.endsWith(".wav") ||
                filePath.endsWith(".aac") || filePath.endsWith(".ogg") || filePath.endsWith(".flac")
    }
}