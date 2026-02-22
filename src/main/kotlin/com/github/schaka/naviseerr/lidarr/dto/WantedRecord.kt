package com.github.schaka.naviseerr.lidarr.dto

data class WantedRecord(
    val id: Long, // albumId
    val artistId: Long,
    val profileId: Long,
    val title: String,
    val foreignAlbumId: String,
    val albumType: String,
    val artist: LidarrArtist,
    val releases: List<LidarrRelease>
)
