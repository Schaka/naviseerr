package com.github.schaka.naviseerr.download.monitor

import com.github.schaka.naviseerr.db.download.enums.DownloadJobStatus
import com.github.schaka.naviseerr.db.download.enums.FileProcessingStatus
import java.util.UUID

data class ProcessableDownload(
    val id: UUID,
    val artistName: String,
    val albumTitle: String?,
    val files: List<ProcessableFile>,
    val status: DownloadJobStatus
)

data class ProcessableFile(
    val id: UUID,
    val filePath: String,
    val acoustidStatus: FileProcessingStatus,
    val postProcessingStatus: FileProcessingStatus,
    val importStatus: FileProcessingStatus
)
