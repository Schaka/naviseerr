package com.github.schaka.naviseerr.download.monitor

import com.github.schaka.naviseerr.db.download.enums.DownloadJobStatus
import com.github.schaka.naviseerr.db.download.enums.FileProcessingStatus
import java.util.UUID

interface DownloadMonitorService {

    fun findReadyForProcessing(): List<ProcessableDownload>

    fun markCompleted(download: ProcessableDownload)

    fun cleanup(download: ProcessableDownload)

    fun updateFileAcoustId(fileId: UUID, status: FileProcessingStatus)

    fun updateFilePostProcessing(fileId: UUID, status: FileProcessingStatus)

    fun updateFileImportStatus(fileId: UUID, status: FileProcessingStatus)

    fun updateJobStatus(jobId: UUID, status: DownloadJobStatus)

    fun needsImport(): Boolean
}
