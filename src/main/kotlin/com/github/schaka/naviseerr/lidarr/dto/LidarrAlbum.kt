package com.github.schaka.naviseerr.lidarr.dto

data class LidarrAlbum(
    val id: Long,
    val artistId: Long,
    val albumType: String,
    val title: String,
    val foreignAlbumId: String,
    var path: String?
)
