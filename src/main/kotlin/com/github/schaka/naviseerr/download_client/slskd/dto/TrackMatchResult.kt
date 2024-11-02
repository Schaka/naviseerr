package com.github.schaka.naviseerr.download_client.slskd.dto

import com.github.schaka.naviseerr.music_library.lidarr.dto.LidarrTrack

data class TrackMatchResult(
    val lidarrTrack: LidarrTrack,
    val file: String?,
    val score: Int,
    val size: Long?,
)