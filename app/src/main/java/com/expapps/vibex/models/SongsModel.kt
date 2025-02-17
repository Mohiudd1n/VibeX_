package com.expapps.vibex.models

data class SongsModel(
    var songId: String? = "",
    var songTitle: String? = "",
    var songArtists: String? = "",
    var songGenre: String? = "",
    var songUrl: String? = "",
    var songThumbnailUrl: String? = "",
    var localSongPath: String? = "",
    var isLocalSong: Boolean = false,
)
