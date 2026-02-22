package com.github.schaka.naviseerr.slskd.dto

import com.github.schaka.naviseerr.lidarr.dto.LidarrTrack

data class TrackMatchResult(
    val lidarrTrack: LidarrTrack,
    val score: Int,
    val file: SearchFile?,
)
