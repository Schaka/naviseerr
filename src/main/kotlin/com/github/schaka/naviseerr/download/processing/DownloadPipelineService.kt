package com.github.schaka.naviseerr.download.processing

import com.github.schaka.naviseerr.db.download.DownloadJob
import com.github.schaka.naviseerr.db.download.DownloadJobFile
import com.github.schaka.naviseerr.db.download.DownloadJobService
import com.github.schaka.naviseerr.db.download.enums.DownloadJobStatus
import com.github.schaka.naviseerr.db.download.enums.DownloadJobType
import com.github.schaka.naviseerr.db.download.enums.FileProcessingStatus
import com.github.schaka.naviseerr.lidarr.LidarrClient
import com.github.schaka.naviseerr.lidarr.dto.LidarrBlacklistRequest
import com.github.schaka.naviseerr.lidarr.dto.LidarrDeleteTrackFilesRequest
import com.github.schaka.naviseerr.lidarr.dto.LidarrSearchCommand
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.TimeUnit

@Service
class DownloadPipelineService(
    private val downloadJobService: DownloadJobService,
    private val acoustIdService: AcoustIdService,
    private val postProcessingService: PostProcessingService,
    private val libraryImportService: LibraryImportService,
    private val lidarrClient: LidarrClient
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    fun process() {
        processAcoustIdPending()
        processAcoustIdFailed()
        processPostProcessing()
        processImportPending()
    }

    private fun processAcoustIdPending() {
        val jobs = downloadJobService.findByStatus(DownloadJobStatus.ACOUSTID_PENDING)
        for (job in jobs) {
            val files = downloadJobService.findFilesForJob(job.id)
            for (file in files) {
                val result = acoustIdService.recognize(file.filePath)
                val status = if (result == AcoustIdResult.RECOGNIZED) FileProcessingStatus.RECOGNIZED else FileProcessingStatus.UNRECOGNIZED
                downloadJobService.updateFileAcoustId(file.id, status)
            }
            val updatedFiles = downloadJobService.findFilesForJob(job.id)
            val allRecognized = updatedFiles.all { it.acoustidStatus == FileProcessingStatus.RECOGNIZED }
            val anyUnrecognized = updatedFiles.any { it.acoustidStatus == FileProcessingStatus.UNRECOGNIZED }
            when {
                allRecognized -> {
                    log.info("All files recognized for job {}, advancing to POST_PROCESSING", job.id)
                    downloadJobService.updateStatus(job.id, DownloadJobStatus.POST_PROCESSING)
                }
                anyUnrecognized -> {
                    log.info("Unrecognized files for job {}, advancing to ACOUSTID_FAILED", job.id)
                    downloadJobService.updateStatus(job.id, DownloadJobStatus.ACOUSTID_FAILED)
                }
            }
        }
    }

    private fun processAcoustIdFailed() {
        val jobs = downloadJobService.findByStatus(DownloadJobStatus.ACOUSTID_FAILED)
        for (job in jobs) {
            val retryCount = downloadJobService.getRetryCount(job.id)
            if (retryCount >= 3) {
                log.warn("Job {} exceeded max retries, marking as FAILED", job.id)
                downloadJobService.updateStatus(job.id, DownloadJobStatus.FAILED)
                continue
            }

            blacklistAndRetrigger(job)
            downloadJobService.resetFilesForRetry(job.id)
            downloadJobService.incrementRetryCount(job.id, DownloadJobStatus.ACOUSTID_PENDING)
            log.info("Retrying AcoustID for job {} (retry {})", job.id, retryCount + 1)
        }
    }

    private fun blacklistAndRetrigger(job: DownloadJob) {
        if (job.jobType == DownloadJobType.LIDARR) {
            val lidarrAlbumId = job.lidarrAlbumId ?: return
            val lidarrArtistId = job.lidarrArtistId ?: return
            val historyId = job.lidarrHistoryId ?: return

            val history = lidarrClient.getAlbumHistory(lidarrAlbumId)
            val record = history.find { it.id == historyId } ?: return

            downloadJobService.addBlacklistEntry(job.id, record.sourceTitle)

            val trackFiles = lidarrClient.getTrackFiles(lidarrAlbumId)
            if (trackFiles.isNotEmpty()) {
                lidarrClient.deleteTrackFiles(LidarrDeleteTrackFilesRequest(trackFiles.map { it.id }))
            }

            lidarrClient.addToBlacklist(
                LidarrBlacklistRequest(
                    artistId = lidarrArtistId,
                    albumIds = listOf(lidarrAlbumId),
                    sourceTitle = record.sourceTitle,
                    quality = null,
                    indexer = record.data.indexer,
                    message = "AcoustID fingerprinting failed after ${job.retryCount} attempts",
                    date = Instant.now().toString()
                )
            )

            lidarrClient.searchAlbums(LidarrSearchCommand("AlbumSearch", albumIds = listOf(lidarrAlbumId)))
            log.info("Blacklisted and re-triggered search for Lidarr album {}", lidarrAlbumId)
        } else {
            val identifier = "${job.slskdUsername}:${job.albumTitle}"
            downloadJobService.addBlacklistEntry(job.id, identifier)
            log.info("slskd re-search needed for job {} ({})", job.id, identifier)
        }
    }

    private fun processPostProcessing() {
        val jobs = downloadJobService.findByStatus(DownloadJobStatus.POST_PROCESSING)
        for (job in jobs) {
            val files = downloadJobService.findFilesForJob(job.id)
            postProcessingService.process(job, files)
            files.forEach { downloadJobService.updateFilePostProcessing(it.id, FileProcessingStatus.COMPLETED) }

            val nextStatus = if (job.jobType == DownloadJobType.LIDARR) DownloadJobStatus.COMPLETED else DownloadJobStatus.IMPORT_PENDING
            log.info("Post-processing done for job {}, advancing to {}", job.id, nextStatus)
            downloadJobService.updateStatus(job.id, nextStatus)
        }
    }

    private fun processImportPending() {
        val jobs = downloadJobService.findByStatus(DownloadJobStatus.IMPORT_PENDING)
        for (job in jobs) {
            val files = downloadJobService.findFilesForJob(job.id)
            libraryImportService.importToLibrary(job, files)
            files.forEach { downloadJobService.updateFileImportStatus(it.id, FileProcessingStatus.COMPLETED) }
            log.info("Import done for job {}, marking COMPLETED", job.id)
            downloadJobService.updateStatus(job.id, DownloadJobStatus.COMPLETED)
        }
    }
}
