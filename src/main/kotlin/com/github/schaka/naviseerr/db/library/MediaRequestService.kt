package com.github.schaka.naviseerr.db.library

import com.github.schaka.naviseerr.db.library.enums.RequestStatus
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
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
        albumTitle: String?
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
            it[status] = RequestStatus.PENDING.name
            it[createdAt] = now
            it[updatedAt] = now
        }
        MediaRequest(id, userId, null, null, musicbrainzArtistId, musicbrainzAlbumId, artistName, albumTitle, RequestStatus.PENDING, null, null, now, now)
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

    fun findPendingByMusicbrainzArtistId(userId: UUID, mbArtistId: String): MediaRequest? = transaction {
        MediaRequests.selectAll()
            .where {
                (MediaRequests.userId eq userId) and
                    (MediaRequests.musicbrainzArtistId eq mbArtistId) and
                    (MediaRequests.status eq RequestStatus.PENDING.name)
            }
            .singleOrNull()
            ?.let(::mapRow)
    }

    fun findPendingByMusicbrainzAlbumId(userId: UUID, mbAlbumId: String): MediaRequest? = transaction {
        MediaRequests.selectAll()
            .where {
                (MediaRequests.userId eq userId) and
                    (MediaRequests.musicbrainzAlbumId eq mbAlbumId) and
                    (MediaRequests.status eq RequestStatus.PENDING.name)
            }
            .singleOrNull()
            ?.let(::mapRow)
    }

    fun updateStatus(
        requestId: UUID,
        status: RequestStatus,
        lidarrArtistId: Long? = null,
        lidarrAlbumId: Long? = null
    ): MediaRequest = transaction {
        MediaRequests.update({ MediaRequests.id eq requestId }) {
            it[this.status] = status.name
            it[updatedAt] = Instant.now()
            if (lidarrArtistId != null) it[this.lidarrArtistId] = lidarrArtistId
            if (lidarrAlbumId != null) it[this.lidarrAlbumId] = lidarrAlbumId
        }
        MediaRequests.selectAll()
            .where { MediaRequests.id eq requestId }
            .single()
            .let(::mapRow)
    }

    private fun mapRow(row: ResultRow) = MediaRequest(
        id = row[MediaRequests.id].value,
        userId = row[MediaRequests.userId].value,
        artistId = row[MediaRequests.artistId]?.value,
        albumId = row[MediaRequests.albumId]?.value,
        musicbrainzArtistId = row[MediaRequests.musicbrainzArtistId],
        musicbrainzAlbumId = row[MediaRequests.musicbrainzAlbumId],
        artistName = row[MediaRequests.artistName],
        albumTitle = row[MediaRequests.albumTitle],
        status = RequestStatus.valueOf(row[MediaRequests.status]),
        lidarrArtistId = row[MediaRequests.lidarrArtistId],
        lidarrAlbumId = row[MediaRequests.lidarrAlbumId],
        createdAt = row[MediaRequests.createdAt],
        updatedAt = row[MediaRequests.updatedAt]
    )
}
