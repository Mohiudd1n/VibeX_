package com.expapps.vibex.player

data class SongInfo(
    var songUrl: String? = null,
    var songTitle: String? = null,
    var artist: String? = null,
    var genreTags: String? = null,
    var totalDuration: String? = null,
    var currentDuration: String? = null,
    var isPlaying: Boolean? = false,
    var isPrepared: Boolean = false
)
