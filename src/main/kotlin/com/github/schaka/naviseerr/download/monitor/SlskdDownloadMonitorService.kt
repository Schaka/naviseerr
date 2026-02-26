package com.github.schaka.naviseerr.download.monitor

import com.github.schaka.naviseerr.slskd.download.SlskdDownloadJob
import com.github.schaka.naviseerr.db.download.SlskdDownloadJobFiles
import com.github.schaka.naviseerr.db.download.SlskdDownloadJobs
import com.github.schaka.naviseerr.db.download.enums.DownloadJobStatus
import com.github.schaka.naviseerr.db.download.enums.FileProcessingStatus
import com.github.schaka.naviseerr.slskd.SoulseekClient
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class SlskdDownloadMonitorService(
    private val soulseekClient: SoulseekClient
) : DownloadMonitorService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    fun checkSlskdDownloads() {
        val activeJobs = findByStatus(DownloadJobStatus.DOWNLOADING)

        for (job in activeJobs) {
            val userDownloads = soulseekClient.getDownloads(job.slskdUsername)
            val allComplete = userDownloads.directories.all { it.isComplete() }

            if (allComplete) {
                log.info("slskd download complete for job {} (user: {}), advancing to ACOUSTID_PENDING", job.id, job.slskdUsername)
                updateJobStatus(job.id, DownloadJobStatus.ACOUSTID_PENDING)
            }
        }
    }

    override fun findReadyForProcessing(): List<ProcessableDownload> = transaction {
        SlskdDownloadJobs.selectAll()
            .where {
                (SlskdDownloadJobs.status eq DownloadJobStatus.ACOUSTID_PENDING) or
                (SlskdDownloadJobs.status eq DownloadJobStatus.POST_PROCESSING) or
                (SlskdDownloadJobs.status eq DownloadJobStatus.IMPORT_PENDING)
            }
            .map { row ->
                val jobId = row[SlskdDownloadJobs.id].value
                val files = SlskdDownloadJobFiles.selectAll()
                    .where { SlskdDownloadJobFiles.jobId eq jobId }
                    .map { fileRow ->
                        ProcessableFile(
                            id = fileRow[SlskdDownloadJobFiles.id].value,
                            filePath = fileRow[SlskdDownloadJobFiles.filePath],
                            acoustidStatus = fileRow[SlskdDownloadJobFiles.acoustidStatus],
                            postProcessingStatus = fileRow[SlskdDownloadJobFiles.postProcessingStatus],
                            importStatus = fileRow[SlskdDownloadJobFiles.importStatus]
                        )
                    }
                ProcessableDownload(
                    id = jobId,
                    artistName = row[SlskdDownloadJobs.artistName],
                    albumTitle = row[SlskdDownloadJobs.albumTitle],
                    files = files,
                    status = row[SlskdDownloadJobs.status]
                )
            }
    }

    override fun markCompleted(download: ProcessableDownload) {
        updateJobStatus(download.id, DownloadJobStatus.COMPLETED)
    }

    override fun cleanup(download: ProcessableDownload): Unit = transaction {
        SlskdDownloadJobFiles.deleteWhere { SlskdDownloadJobFiles.jobId eq download.id }
        SlskdDownloadJobs.deleteWhere { SlskdDownloadJobs.id eq download.id }
    }

    override fun updateFileAcoustId(fileId: UUID, status: FileProcessingStatus): Unit = transaction {
        SlskdDownloadJobFiles.update({ SlskdDownloadJobFiles.id eq fileId }) {
            it[acoustidStatus] = status
            it[updatedAt] = Instant.now()
        }
    }

    override fun updateFilePostProcessing(fileId: UUID, status: FileProcessingStatus): Unit = transaction {
        SlskdDownloadJobFiles.update({ SlskdDownloadJobFiles.id eq fileId }) {
            it[postProcessingStatus] = status
            it[updatedAt] = Instant.now()
        }
    }

    override fun updateFileImportStatus(fileId: UUID, status: FileProcessingStatus): Unit = transaction {
        SlskdDownloadJobFiles.update({ SlskdDownloadJobFiles.id eq fileId }) {
            it[importStatus] = status
            it[updatedAt] = Instant.now()
        }
    }

    override fun updateJobStatus(jobId: UUID, status: DownloadJobStatus): Unit = transaction {
        SlskdDownloadJobs.update({ SlskdDownloadJobs.id eq jobId }) {
            it[this.status] = status
            it[updatedAt] = Instant.now()
        }
    }

    override fun needsImport(): Boolean = true

    fun createJob(
        artistName: String,
        albumTitle: String?,
        mbArtistId: String?,
        mbAlbumId: String?,
        slskdUsername: String,
        filePaths: List<String>
    ): SlskdDownloadJob = transaction {
        val id = UUID.randomUUID()
        val now = Instant.now()
        SlskdDownloadJobs.insert {
            it[this.id] = id
            it[status] = DownloadJobStatus.DOWNLOADING
            it[this.artistName] = artistName
            it[this.albumTitle] = albumTitle
            it[musicbrainzArtistId] = mbArtistId
            it[musicbrainzAlbumId] = mbAlbumId
            it[this.slskdUsername] = slskdUsername
            it[createdAt] = now
            it[updatedAt] = now
        }
        insertFiles(id, filePaths, now)
        SlskdDownloadJob(id, DownloadJobStatus.DOWNLOADING, artistName, albumTitle, mbArtistId, mbAlbumId, slskdUsername, now, now)
    }

    private fun findByStatus(status: DownloadJobStatus): List<SlskdDownloadJob> = transaction {
        SlskdDownloadJobs.selectAll()
            .where { SlskdDownloadJobs.status eq status }
            .map { row ->
                SlskdDownloadJob(
                    id = row[SlskdDownloadJobs.id].value,
                    status = row[SlskdDownloadJobs.status],
                    artistName = row[SlskdDownloadJobs.artistName],
                    albumTitle = row[SlskdDownloadJobs.albumTitle],
                    musicbrainzArtistId = row[SlskdDownloadJobs.musicbrainzArtistId],
                    musicbrainzAlbumId = row[SlskdDownloadJobs.musicbrainzAlbumId],
                    slskdUsername = row[SlskdDownloadJobs.slskdUsername],
                    createdAt = row[SlskdDownloadJobs.createdAt],
                    updatedAt = row[SlskdDownloadJobs.updatedAt]
                )
            }
    }

    private fun insertFiles(jobId: UUID, filePaths: List<String>, now: Instant) {
        filePaths.forEach { path ->
            SlskdDownloadJobFiles.insert {
                it[id] = UUID.randomUUID()
                it[this.jobId] = jobId
                it[filePath] = path
                it[acoustidStatus] = FileProcessingStatus.PENDING
                it[postProcessingStatus] = FileProcessingStatus.PENDING
                it[importStatus] = FileProcessingStatus.PENDING
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }
}
