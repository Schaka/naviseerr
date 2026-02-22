package com.github.schaka.naviseerr.lidarr.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class LidarrHistoryRecord(
    val id: Long,
    val albumId: Long,
    val artistId: Long,
    val sourceTitle: String,
    val date: String,
    val downloadId: String?,
    val eventType: String,
    val data: LidarrHistoryData
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LidarrHistoryData(
    val downloadClient: String? = null,
    val downloadClientName: String? = null,
    val droppedPath: String? = null,
    val importedPath: String? = null,
    val indexer: String? = null,
    val torrentInfoHash: String? = null
)
