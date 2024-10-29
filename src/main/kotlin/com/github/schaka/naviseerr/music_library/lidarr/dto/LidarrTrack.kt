package com.github.schaka.naviseerr.music_library.lidarr.dto

data class LidarrTrack(
    val title: String,
    val duration: Long,
    val foreignTrackId: String
)
