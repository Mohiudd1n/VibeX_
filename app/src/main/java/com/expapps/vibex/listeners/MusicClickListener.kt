package com.expapps.vibex.listeners

import com.expapps.vibex.models.SongsModel

fun interface MusicClickListener {
    fun onClick(filePath: String, songUrl: String, songTitle: String, artistName: String, songsModel: SongsModel)
}