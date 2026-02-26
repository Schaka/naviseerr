package com.github.schaka.naviseerr.db.download

import com.github.schaka.naviseerr.db.download.enums.FileProcessingStatus
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp

object SlskdDownloadJobFiles : UUIDTable("slskd_download_job_files") {
    val jobId = reference("job_id", SlskdDownloadJobs)
    val filePath = varchar("file_path", 1024)
    val acoustidStatus = enumerationByName<FileProcessingStatus>("acoustid_status", 32).default(FileProcessingStatus.PENDING)
    val postProcessingStatus = enumerationByName<FileProcessingStatus>("post_processing_status", 32).default(FileProcessingStatus.PENDING)
    val importStatus = enumerationByName<FileProcessingStatus>("import_status", 32).default(FileProcessingStatus.PENDING)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
