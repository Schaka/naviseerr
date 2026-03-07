package com.github.schaka.naviseerr.lidarr.dto

import com.github.schaka.naviseerr.lidarr.dto.quality.Quality

data class LidarrBlacklistRequest(
    val artistId: Long,
    val albumIds: List<Long>,
    val sourceTitle: String,
    val quality: Quality?,
    val indexer: String?,
    val message: String,
    val date: String
)
