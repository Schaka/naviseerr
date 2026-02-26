package com.github.schaka.naviseerr.download.monitor

import com.github.schaka.naviseerr.lidarr.download.LidarrDownloadJob
import com.github.schaka.naviseerr.db.download.LidarrDownloadJobFiles
import com.github.schaka.naviseerr.db.download.LidarrDownloadJobs
import com.github.schaka.naviseerr.db.download.MediaRequestDownloadJobs
import com.github.schaka.naviseerr.db.download.enums.DownloadJobStatus
import com.github.schaka.naviseerr.db.download.enums.DownloadProtocol
import com.github.schaka.naviseerr.db.download.enums.FileProcessingStatus
import com.github.schaka.naviseerr.db.library.MediaRequest
import com.github.schaka.naviseerr.db.library.MediaRequestService
import com.github.schaka.naviseerr.lidarr.LidarrClient
import com.github.schaka.naviseerr.lidarr.dto.LidarrDownloadClient
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.or
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
class LidarrDownloadMonitorService(
    private val lidarrClient: LidarrClient,
    private val mediaRequestService: MediaRequestService
) : DownloadMonitorService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.MINUTES)
    fun checkLidarrHistory() {
        val downloadClients = lidarrClient.getDownloadClients()
        val knownHistoryIds = findAllLidarrHistoryIds()

        mediaRequestService.findLidarrRequests()
            .forEach { processRequest(it, downloadClients, knownHistoryIds) }
    }

    override fun findReadyForProcessing(): List<ProcessableDownload> = transaction {
        LidarrDownloadJobs.selectAll()
            .where {
                (LidarrDownloadJobs.status eq DownloadJobStatus.ACOUSTID_PENDING) or
                (LidarrDownloadJobs.status eq DownloadJobStatus.POST_PROCESSING)
            }
            .map { row ->
                val jobId = row[LidarrDownloadJobs.id].value
                val files = LidarrDownloadJobFiles.selectAll()
                    .where { LidarrDownloadJobFiles.jobId eq jobId }
                    .map { fileRow ->
                        ProcessableFile(
                            id = fileRow[LidarrDownloadJobFiles.id].value,
                            filePath = fileRow[LidarrDownloadJobFiles.filePath],
                            acoustidStatus = fileRow[LidarrDownloadJobFiles.acoustidStatus],
                            postProcessingStatus = fileRow[LidarrDownloadJobFiles.postProcessingStatus],
                            importStatus = FileProcessingStatus.COMPLETED
                        )
                    }
                ProcessableDownload(
                    id = jobId,
                    artistName = row[LidarrDownloadJobs.artistName],
                    albumTitle = row[LidarrDownloadJobs.albumTitle],
                    files = files,
                    status = row[LidarrDownloadJobs.status]
                )
            }
    }

    override fun markCompleted(download: ProcessableDownload) {
        updateJobStatus(download.id, DownloadJobStatus.COMPLETED)
    }

    override fun cleanup(download: ProcessableDownload): Unit = transaction {
        // Nothing to do for now
        // LidarrDownloadJobFiles.deleteWhere { LidarrDownloadJobFiles.jobId eq download.id }
    }

    override fun updateFileAcoustId(fileId: UUID, status: FileProcessingStatus): Unit = transaction {
        LidarrDownloadJobFiles.update({ LidarrDownloadJobFiles.id eq fileId }) {
            it[acoustidStatus] = status
            it[updatedAt] = Instant.now()
        }
    }

    override fun updateFilePostProcessing(fileId: UUID, status: FileProcessingStatus): Unit = transaction {
        LidarrDownloadJobFiles.update({ LidarrDownloadJobFiles.id eq fileId }) {
            it[postProcessingStatus] = status
            it[updatedAt] = Instant.now()
        }
    }

    override fun updateFileImportStatus(fileId: UUID, status: FileProcessingStatus) {
        // Lidarr handles its own import — no-op
    }

    override fun updateJobStatus(jobId: UUID, status: DownloadJobStatus): Unit = transaction {
        LidarrDownloadJobs.update({ LidarrDownloadJobs.id eq jobId }) {
            it[this.status] = status
            it[updatedAt] = Instant.now()
        }
    }

    override fun needsImport(): Boolean = false

    fun createJob(
        mediaRequestId: UUID,
        artistName: String,
        albumTitle: String?,
        mbArtistId: String?,
        mbAlbumId: String?,
        lidarrArtistId: Long,
        lidarrAlbumId: Long,
        lidarrHistoryId: Long,
        downloadClient: String?,
        protocol: DownloadProtocol,
        filePaths: List<String>
    ): LidarrDownloadJob = transaction {
        val id = UUID.randomUUID()
        val now = Instant.now()

        MediaRequestDownloadJobs.insert {
            it[this.id] = id
            it[this.mediaRequestId] = mediaRequestId
            it[jobType] = "LIDARR"
            it[createdAt] = now
        }

        LidarrDownloadJobs.insert {
            it[this.id] = id
            it[status] = DownloadJobStatus.ACOUSTID_PENDING
            it[this.artistName] = artistName
            it[this.albumTitle] = albumTitle
            it[musicbrainzArtistId] = mbArtistId
            it[musicbrainzAlbumId] = mbAlbumId
            it[this.lidarrArtistId] = lidarrArtistId
            it[this.lidarrAlbumId] = lidarrAlbumId
            it[this.lidarrHistoryId] = lidarrHistoryId
            it[this.downloadClient] = downloadClient
            it[downloadProtocol] = protocol
            it[createdAt] = now
            it[updatedAt] = now
        }
        insertFiles(id, filePaths, now)
        LidarrDownloadJob(id, DownloadJobStatus.ACOUSTID_PENDING, artistName, albumTitle, mbArtistId, mbAlbumId, lidarrArtistId, lidarrAlbumId, lidarrHistoryId, downloadClient, protocol, now, now)
    }

    private fun processRequest(
        request: MediaRequest,
        downloadClients: List<LidarrDownloadClient>,
        knownHistoryIds: Set<Long>
    ) {
        val history = lidarrClient.getArtistHistory(request.lidarrArtistId!!)

        val relevantHistory = if (request.lidarrAlbumId != null) {
            history.filter { it.albumId == request.lidarrAlbumId }
        } else {
            history
        }

        relevantHistory
            .filter { it.downloadId != null }
            .groupBy { it.downloadId!! }
            .forEach { (_, group) ->
                val importedRecord = group.find { it.eventType == "downloadImported" } ?: return@forEach

                if (importedRecord.id in knownHistoryIds) {
                    return@forEach
                }

                val clientName = group
                    .filter { it.eventType == "trackFileImported" }
                    .firstNotNullOfOrNull { it.data.downloadClient }
                val protocol = resolveProtocol(clientName, downloadClients)

                val filePaths = lidarrClient.getTrackFiles(importedRecord.albumId).map { it.path }

                log.info(
                    "Creating download job for request {} album {} history record {} (protocol: {})",
                    request.id, importedRecord.albumId, importedRecord.id, protocol
                )
                createJob(
                    mediaRequestId = request.id,
                    artistName = request.artistName,
                    albumTitle = request.albumTitle,
                    mbArtistId = request.musicbrainzArtistId,
                    mbAlbumId = request.musicbrainzAlbumId,
                    lidarrArtistId = request.lidarrArtistId,
                    lidarrAlbumId = importedRecord.albumId,
                    lidarrHistoryId = importedRecord.id,
                    downloadClient = clientName,
                    protocol = protocol,
                    filePaths = filePaths
                )
            }
    }

    private fun resolveProtocol(clientName: String?, downloadClients: List<LidarrDownloadClient>): DownloadProtocol {
        val client = downloadClients.find { it.name == clientName } ?: return DownloadProtocol.UNKNOWN
        return when {
            client.protocol == "SoulseekDownloadProtocol" -> DownloadProtocol.SOULSEEK
            client.protocol == "torrent" -> DownloadProtocol.TORRENT
            client.protocol == "usenet" -> DownloadProtocol.USENET
            else -> DownloadProtocol.UNKNOWN
        }
    }

    private fun findAllLidarrHistoryIds(): Set<Long> = transaction {
        LidarrDownloadJobs.selectAll()
            .where { LidarrDownloadJobs.lidarrHistoryId.isNotNull() }
            .map { it[LidarrDownloadJobs.lidarrHistoryId] }
            .toHashSet()
    }

    private fun insertFiles(jobId: UUID, filePaths: List<String>, now: Instant) {
        filePaths.forEach { path ->
            LidarrDownloadJobFiles.insert {
                it[id] = UUID.randomUUID()
                it[this.jobId] = jobId
                it[filePath] = path
                it[acoustidStatus] = FileProcessingStatus.PENDING
                it[postProcessingStatus] = FileProcessingStatus.PENDING
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }
}
