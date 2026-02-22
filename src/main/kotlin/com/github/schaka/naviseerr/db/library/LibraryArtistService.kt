package com.github.schaka.naviseerr.db.library

import com.github.schaka.naviseerr.db.library.enums.MediaSource
import com.github.schaka.naviseerr.db.library.enums.MediaStatus
import com.github.schaka.naviseerr.lidarr.dto.LidarrArtist
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class LibraryArtistService {

    fun upsertFromLidarr(artist: LidarrArtist, allAlbumsAvailable: Boolean, searchTriggered: Boolean = false): LibraryArtist = transaction {
        val status = deriveStatus(artist, allAlbumsAvailable)
        val existing = LibraryArtists.selectAll()
            .where { LibraryArtists.lidarrId eq artist.id }
            .singleOrNull()

        val lastSearchedAt = if (searchTriggered) Instant.now() else null

        if (existing != null) {
            val preservedLastSearchedAt = existing[LibraryArtists.lastSearchedAt]
            LibraryArtists.update({ LibraryArtists.lidarrId eq artist.id }) {
                it[name] = artist.artistName
                it[cleanName] = artist.cleanName
                it[musicbrainzId] = artist.foreignArtistId
                it[this.status] = status
                it[syncedAt] = Instant.now()
                it[this.lastSearchedAt] = lastSearchedAt ?: preservedLastSearchedAt
            }
            return@transaction mapRow(existing).copy(
                name = artist.artistName,
                cleanName = artist.cleanName,
                musicbrainzId = artist.foreignArtistId,
                status = status,
                syncedAt = Instant.now(),
                lastSearchedAt = lastSearchedAt ?: preservedLastSearchedAt
            )
        }

        val id = UUID.randomUUID()
        LibraryArtists.insert {
            it[this.id] = id
            it[musicbrainzId] = artist.foreignArtistId
            it[lidarrId] = artist.id
            it[name] = artist.artistName
            it[cleanName] = artist.cleanName
            it[this.status] = status
            it[mediaSource] = MediaSource.LIDARR
            it[syncedAt] = Instant.now()
            it[this.lastSearchedAt] = lastSearchedAt
        }
        LibraryArtist(id, artist.foreignArtistId, artist.id, artist.artistName, artist.cleanName, status, MediaSource.LIDARR, Instant.now(), lastSearchedAt)
    }

    fun updateAfterSearch(lidarrId: Long, at: Instant): Unit = transaction {
        LibraryArtists.update({ LibraryArtists.lidarrId eq lidarrId }) {
            it[status] = MediaStatus.MONITORED
            it[lastSearchedAt] = at
        }
    }

    fun findByMusicbrainzId(mbId: String): LibraryArtist? = transaction {
        LibraryArtists.selectAll()
            .where { LibraryArtists.musicbrainzId eq mbId }
            .singleOrNull()
            ?.let(::mapRow)
    }

    fun findAll(): List<LibraryArtist> = transaction {
        LibraryArtists.selectAll().map(::mapRow)
    }

    private fun deriveStatus(artist: LidarrArtist, allAlbumsAvailable: Boolean): MediaStatus {
        if (allAlbumsAvailable) return MediaStatus.AVAILABLE
        return if (artist.monitored) MediaStatus.MONITORED else MediaStatus.UNMONITORED
    }

    private fun mapRow(row: ResultRow) = LibraryArtist(
        id = row[LibraryArtists.id].value,
        musicbrainzId = row[LibraryArtists.musicbrainzId],
        lidarrId = row[LibraryArtists.lidarrId],
        name = row[LibraryArtists.name],
        cleanName = row[LibraryArtists.cleanName],
        status = row[LibraryArtists.status],
        source = row[LibraryArtists.mediaSource],
        syncedAt = row[LibraryArtists.syncedAt],
        lastSearchedAt = row[LibraryArtists.lastSearchedAt]
    )
}
