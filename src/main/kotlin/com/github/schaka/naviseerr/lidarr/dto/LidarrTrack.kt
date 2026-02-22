package com.github.schaka.naviseerr.lidarr.dto

data class LidarrTrack(
    val id: Long,
    val title: String,
    val duration: Long,
    val foreignTrackId: String
)
