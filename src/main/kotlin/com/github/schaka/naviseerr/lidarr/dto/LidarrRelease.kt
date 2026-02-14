package com.github.schaka.naviseerr.lidarr.dto

data class LidarrRelease(
    val id: Long,
    val albumId: Long,
    val foreignReleaseId: String,
    val title: String,
    val monitored: Boolean,
)
