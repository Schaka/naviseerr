package com.github.schaka.naviseerr.db.library

import com.github.schaka.naviseerr.db.download.MediaRequestDownloadJobs
import com.github.schaka.naviseerr.db.library.enums.RequestStatus
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class MediaRequestService {

    fun create(
        userId: UUID,
        musicbrainzArtistId: String,
        musicbrainzAlbumId: String?,
        artistName: String,
        albumTitle: String?,
        lidarrArtistId: Long? = null,
        lidarrAlbumId: Long? = null
    ): MediaRequest = transaction {
        val id = UUID.randomUUID()
        val now = Instant.now()
        MediaRequests.insert {
            it[this.id] = id
            it[this.userId] = userId
            it[this.musicbrainzArtistId] = musicbrainzArtistId
            it[this.musicbrainzAlbumId] = musicbrainzAlbumId
            it[this.artistName] = artistName
            it[this.albumTitle] = albumTitle
            it[status] = RequestStatus.REQUESTED
            it[createdAt] = now
            it[updatedAt] = now
            if (lidarrArtistId != null) it[this.lidarrArtistId] = lidarrArtistId
            if (lidarrAlbumId != null) it[this.lidarrAlbumId] = lidarrAlbumId
        }
        MediaRequest(id, userId,  musicbrainzArtistId, musicbrainzAlbumId, artistName, albumTitle, RequestStatus.REQUESTED, lidarrArtistId, lidarrAlbumId, now, now)
    }

    fun findByUser(userId: UUID): List<MediaRequest> = transaction {
        MediaRequests.selectAll()
            .where { MediaRequests.userId eq userId }
            .orderBy(MediaRequests.createdAt)
            .map(::mapRow)
    }

    fun findAll(): List<MediaRequest> = transaction {
        MediaRequests.selectAll()
            .orderBy(MediaRequests.createdAt)
            .map(::mapRow)
    }

    fun findLidarrRequests(): List<MediaRequest> = transaction {
        MediaRequests
            .join(
                MediaRequestDownloadJobs,
                JoinType.LEFT,
                onColumn = MediaRequests.id,
                otherColumn = MediaRequestDownloadJobs.mediaRequestId,
                additionalConstraint = { MediaRequestDownloadJobs.jobType eq "LIDARR" }
            )
            .selectAll()
            .where {
                MediaRequests.lidarrArtistId.isNotNull() and
                (MediaRequests.status neq RequestStatus.FAILED) and
                MediaRequestDownloadJobs.id.isNull()
            }
            .map(::mapRow)
    }

    fun findActiveByMusicbrainzArtistId(mbArtistId: String): MediaRequest? = transaction {
        MediaRequests.selectAll()
            .where {
                    (MediaRequests.musicbrainzArtistId eq mbArtistId) and
                    (MediaRequests.status eq RequestStatus.REQUESTED)
            }
            .singleOrNull()
            ?.let(::mapRow)
    }

    fun findActiveByMusicbrainzAlbumId(mbAlbumId: String): MediaRequest? = transaction {
        MediaRequests.selectAll()
            .where {
                    (MediaRequests.musicbrainzAlbumId eq mbAlbumId) and
                    (MediaRequests.status eq RequestStatus.REQUESTED)
            }
            .singleOrNull()
            ?.let(::mapRow)
    }

    fun updateAllActiveToAvailableByLidarrAlbumId(lidarrAlbumId: Long): Unit = transaction {
        MediaRequests.update({
            (MediaRequests.lidarrAlbumId eq lidarrAlbumId) and
                (MediaRequests.status eq RequestStatus.REQUESTED)
        }) {
            it[status] = RequestStatus.AVAILABLE
            it[updatedAt] = Instant.now()
        }
    }

    fun updateAllActiveToAvailableByLidarrArtistId(lidarrArtistId: Long): Unit = transaction {
        MediaRequests.update({
            (MediaRequests.lidarrArtistId eq lidarrArtistId) and
                (MediaRequests.status eq RequestStatus.REQUESTED)
        }) {
            it[status] = RequestStatus.AVAILABLE
            it[updatedAt] = Instant.now()
        }
    }

    private fun mapRow(row: ResultRow) = MediaRequest(
        id = row[MediaRequests.id].value,
        userId = row[MediaRequests.userId].value,
        musicbrainzArtistId = row[MediaRequests.musicbrainzArtistId],
        musicbrainzAlbumId = row[MediaRequests.musicbrainzAlbumId],
        artistName = row[MediaRequests.artistName],
        albumTitle = row[MediaRequests.albumTitle],
        status = row[MediaRequests.status],
        lidarrArtistId = row[MediaRequests.lidarrArtistId],
        lidarrAlbumId = row[MediaRequests.lidarrAlbumId],
        createdAt = row[MediaRequests.createdAt],
        updatedAt = row[MediaRequests.updatedAt]
    )
}
