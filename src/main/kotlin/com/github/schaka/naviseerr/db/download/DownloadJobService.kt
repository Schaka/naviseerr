package com.github.schaka.naviseerr.db.download

import com.github.schaka.naviseerr.db.download.enums.DownloadJobStatus
import com.github.schaka.naviseerr.db.download.enums.DownloadJobType
import com.github.schaka.naviseerr.db.download.enums.DownloadProtocol
import com.github.schaka.naviseerr.db.download.enums.FileProcessingStatus
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class DownloadJobService {

    fun createLidarrJob(
        mediaRequestId: UUID?,
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
    ): DownloadJob = transaction {
        val id = UUID.randomUUID()
        val now = Instant.now()
        DownloadJobs.insert {
            it[this.id] = id
            it[this.mediaRequestId] = mediaRequestId
            it[jobType] = DownloadJobType.LIDARR
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
            it[retryCount] = 0
            it[createdAt] = now
            it[updatedAt] = now
        }
        insertFiles(id, filePaths, now)
        DownloadJob(id, mediaRequestId, DownloadJobType.LIDARR, DownloadJobStatus.ACOUSTID_PENDING, artistName, albumTitle, mbArtistId, mbAlbumId, lidarrArtistId, lidarrAlbumId, lidarrHistoryId, downloadClient, protocol, null, 0, now, now)
    }

    fun createSlskdJob(
        artistName: String,
        albumTitle: String?,
        mbArtistId: String?,
        mbAlbumId: String?,
        slskdUsername: String,
        filePaths: List<String>
    ): DownloadJob = transaction {
        val id = UUID.randomUUID()
        val now = Instant.now()
        DownloadJobs.insert {
            it[this.id] = id
            it[mediaRequestId] = null
            it[jobType] = DownloadJobType.SLSKD
            it[status] = DownloadJobStatus.DOWNLOADING
            it[this.artistName] = artistName
            it[this.albumTitle] = albumTitle
            it[musicbrainzArtistId] = mbArtistId
            it[musicbrainzAlbumId] = mbAlbumId
            it[downloadProtocol] = DownloadProtocol.SOULSEEK
            it[this.slskdUsername] = slskdUsername
            it[retryCount] = 0
            it[createdAt] = now
            it[updatedAt] = now
        }
        insertFiles(id, filePaths, now)
        DownloadJob(id, null, DownloadJobType.SLSKD, DownloadJobStatus.DOWNLOADING, artistName, albumTitle, mbArtistId, mbAlbumId, null, null, null, null, DownloadProtocol.SOULSEEK, slskdUsername, 0, now, now)
    }

    fun findByStatus(status: DownloadJobStatus): List<DownloadJob> = transaction {
        DownloadJobs.selectAll()
            .where { DownloadJobs.status eq status }
            .map(::mapRow)
    }

    fun findByTypeAndStatus(type: DownloadJobType, status: DownloadJobStatus): List<DownloadJob> = transaction {
        DownloadJobs.selectAll()
            .where { (DownloadJobs.jobType eq type) and (DownloadJobs.status eq status) }
            .map(::mapRow)
    }

    fun updateStatus(id: UUID, status: DownloadJobStatus): Unit = transaction {
        DownloadJobs.update({ DownloadJobs.id eq id }) {
            it[this.status] = status
            it[updatedAt] = Instant.now()
        }
    }

    fun incrementRetryCount(id: UUID, newStatus: DownloadJobStatus): Unit = transaction {
        val current = DownloadJobs.selectAll().where { DownloadJobs.id eq id }.single()
        DownloadJobs.update({ DownloadJobs.id eq id }) {
            it[retryCount] = current[DownloadJobs.retryCount] + 1
            it[status] = newStatus
            it[updatedAt] = Instant.now()
        }
    }

    fun getRetryCount(id: UUID): Int = transaction {
        DownloadJobs.selectAll().where { DownloadJobs.id eq id }.single()[DownloadJobs.retryCount]
    }

    fun findFilesForJob(jobId: UUID): List<DownloadJobFile> = transaction {
        DownloadJobFiles.selectAll()
            .where { DownloadJobFiles.jobId eq jobId }
            .map(::mapFileRow)
    }

    fun updateFileAcoustId(fileId: UUID, status: FileProcessingStatus): Unit = transaction {
        DownloadJobFiles.update({ DownloadJobFiles.id eq fileId }) {
            it[acoustidStatus] = status
            it[updatedAt] = Instant.now()
        }
    }

    fun updateFilePostProcessing(fileId: UUID, status: FileProcessingStatus): Unit = transaction {
        DownloadJobFiles.update({ DownloadJobFiles.id eq fileId }) {
            it[postProcessingStatus] = status
            it[updatedAt] = Instant.now()
        }
    }

    fun updateFileImportStatus(fileId: UUID, status: FileProcessingStatus): Unit = transaction {
        DownloadJobFiles.update({ DownloadJobFiles.id eq fileId }) {
            it[importStatus] = status
            it[updatedAt] = Instant.now()
        }
    }

    fun resetFilesForRetry(jobId: UUID): Unit = transaction {
        DownloadJobFiles.update({ DownloadJobFiles.jobId eq jobId }) {
            it[acoustidStatus] = FileProcessingStatus.PENDING
            it[postProcessingStatus] = FileProcessingStatus.PENDING
            it[importStatus] = FileProcessingStatus.PENDING
            it[updatedAt] = Instant.now()
        }
    }

    fun addBlacklistEntry(jobId: UUID, sourceIdentifier: String): Unit = transaction {
        DownloadSourceBlacklist.insert {
            it[id] = UUID.randomUUID()
            it[this.jobId] = jobId
            it[this.sourceIdentifier] = sourceIdentifier
            it[blacklistedAt] = Instant.now()
        }
    }

    fun findAllLidarrHistoryIds(): Set<Long> = transaction {
        DownloadJobs.selectAll()
            .where { DownloadJobs.lidarrHistoryId.isNotNull() }
            .map { it[DownloadJobs.lidarrHistoryId]!! }
            .toHashSet()
    }

    fun existsByMediaRequestId(mediaRequestId: UUID): Boolean = transaction {
        DownloadJobs.selectAll()
            .where { DownloadJobs.mediaRequestId eq mediaRequestId }
            .any()
    }

    fun existsByLidarrHistoryId(historyId: Long): Boolean = transaction {
        DownloadJobs.selectAll()
            .where { DownloadJobs.lidarrHistoryId eq historyId }
            .any()
    }

    private fun insertFiles(jobId: UUID, filePaths: List<String>, now: Instant) {
        filePaths.forEach { path ->
            DownloadJobFiles.insert {
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

    private fun mapRow(row: org.jetbrains.exposed.v1.core.ResultRow) = DownloadJob(
        id = row[DownloadJobs.id].value,
        mediaRequestId = row[DownloadJobs.mediaRequestId]?.value,
        jobType = row[DownloadJobs.jobType],
        status = row[DownloadJobs.status],
        artistName = row[DownloadJobs.artistName],
        albumTitle = row[DownloadJobs.albumTitle],
        musicbrainzArtistId = row[DownloadJobs.musicbrainzArtistId],
        musicbrainzAlbumId = row[DownloadJobs.musicbrainzAlbumId],
        lidarrArtistId = row[DownloadJobs.lidarrArtistId],
        lidarrAlbumId = row[DownloadJobs.lidarrAlbumId],
        lidarrHistoryId = row[DownloadJobs.lidarrHistoryId],
        downloadClient = row[DownloadJobs.downloadClient],
        downloadProtocol = row[DownloadJobs.downloadProtocol],
        slskdUsername = row[DownloadJobs.slskdUsername],
        retryCount = row[DownloadJobs.retryCount],
        createdAt = row[DownloadJobs.createdAt],
        updatedAt = row[DownloadJobs.updatedAt]
    )

    private fun mapFileRow(row: org.jetbrains.exposed.v1.core.ResultRow) = DownloadJobFile(
        id = row[DownloadJobFiles.id].value,
        jobId = row[DownloadJobFiles.jobId].value,
        filePath = row[DownloadJobFiles.filePath],
        acoustidStatus = row[DownloadJobFiles.acoustidStatus],
        postProcessingStatus = row[DownloadJobFiles.postProcessingStatus],
        importStatus = row[DownloadJobFiles.importStatus],
        createdAt = row[DownloadJobFiles.createdAt],
        updatedAt = row[DownloadJobFiles.updatedAt]
    )
}
