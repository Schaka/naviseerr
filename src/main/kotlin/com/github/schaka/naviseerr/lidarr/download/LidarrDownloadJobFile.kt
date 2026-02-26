package com.github.schaka.naviseerr.lidarr.download

import com.github.schaka.naviseerr.db.download.enums.FileProcessingStatus
import java.time.Instant
import java.util.UUID

data class LidarrDownloadJobFile(
    val id: UUID,
    val jobId: UUID,
    val filePath: String,
    val acoustidStatus: FileProcessingStatus,
    val postProcessingStatus: FileProcessingStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)
