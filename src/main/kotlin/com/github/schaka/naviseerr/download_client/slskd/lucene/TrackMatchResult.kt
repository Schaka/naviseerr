package com.github.schaka.naviseerr.download_client.slskd.lucene

import com.github.schaka.naviseerr.music_library.lidarr.dto.LidarrTrack

data class TrackMatchResult(
    val lidarrTrack: LidarrTrack,
    val file: String?,
    val score: Int
)
