package com.github.schaka.naviseerr.lidarr.dto

data class LidarrAddAlbumRequest(
    val foreignAlbumId: String,
    val title: String,
    val artistId: Long,
    val artist: LidarrArtist,
    val monitored: Boolean = true,
    val addOptions: AddAlbumOptions = AddAlbumOptions(),
)

data class AddAlbumOptions(
    val searchForNewAlbum: Boolean = true
)
