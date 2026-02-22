package com.github.schaka.naviseerr.lidarr.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class LidarrQueueItem(
    val id: Long,
    val artistId: Long,
    val albumId: Long,
    val title: String,
    val status: String,
    val trackedDownloadStatus: String?,
    val trackedDownloadState: String?,
    val statusMessages: List<LidarrStatusMessage> = emptyList(),
    val downloadId: String?,
    val protocol: String?,
    val size: Double,
    val sizeleft: Double,
    val timeleft: String?,
    val estimatedCompletionTime: String?,
    val artist: LidarrArtist?,
    val album: LidarrAlbum?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LidarrStatusMessage(
    val title: String?,
    val messages: List<String> = emptyList()
)
