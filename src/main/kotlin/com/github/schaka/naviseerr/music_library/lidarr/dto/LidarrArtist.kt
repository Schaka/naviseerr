package com.github.schaka.naviseerr.music_library.lidarr.dto

data class LidarrArtist(
    val id: Long,
    val artistName: String,
    val cleanName: String,
    val mbId: String?, // musicbrainz
    val foreignArtistId: String, // musicbrainz
    val path: String,
    val albums: MutableList<LidarrAlbum> = ArrayList(),
)
