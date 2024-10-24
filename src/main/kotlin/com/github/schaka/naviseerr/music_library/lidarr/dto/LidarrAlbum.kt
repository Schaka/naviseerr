package com.github.schaka.naviseerr.music_library.lidarr.dto

data class LidarrAlbum(
    val id: Long,
    val artistId: Long,
    val albumType: String,
    val title: String,
    val foreignAlbumId: String,
    var path: String?
)
