package com.github.schaka.naviseerr.db.download

import com.github.schaka.naviseerr.db.download.enums.FileProcessingStatus
import java.time.Instant
import java.util.UUID

data class DownloadJobFile(
    val id: UUID,
    val jobId: UUID,
    val filePath: String,
    val acoustidStatus: FileProcessingStatus,
    val postProcessingStatus: FileProcessingStatus,
    val importStatus: FileProcessingStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)
