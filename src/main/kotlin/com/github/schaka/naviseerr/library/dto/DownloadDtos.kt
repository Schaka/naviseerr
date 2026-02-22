package com.github.schaka.naviseerr.library.dto

data class DownloadQueueDto(
    val items: List<DownloadItemDto>,
    val totalRecords: Long
)

data class DownloadItemDto(
    val id: Long,
    val artistName: String,
    val albumTitle: String,
    val progress: Double,
    val status: String,
    val timeleft: String?,
    val estimatedCompletionTime: String?,
    val protocol: String?
)
