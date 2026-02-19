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

    fun upsertFromLidarr(artist: LidarrArtist): LibraryArtist = transaction {
        val existing = LibraryArtists.selectAll()
            .where { LibraryArtists.lidarrId eq artist.id }
            .singleOrNull()

        if (existing != null) {
            LibraryArtists.update({ LibraryArtists.lidarrId eq artist.id }) {
                it[name] = artist.artistName
                it[cleanName] = artist.cleanName
                it[musicbrainzId] = artist.foreignArtistId
                it[status] = MediaStatus.AVAILABLE.name
                it[syncedAt] = Instant.now()
            }
            return@transaction mapRow(existing).copy(
                name = artist.artistName,
                cleanName = artist.cleanName,
                musicbrainzId = artist.foreignArtistId,
                status = MediaStatus.AVAILABLE,
                syncedAt = Instant.now()
            )
        }

        val id = UUID.randomUUID()
        LibraryArtists.insert {
            it[this.id] = id
            it[musicbrainzId] = artist.foreignArtistId
            it[lidarrId] = artist.id
            it[name] = artist.artistName
            it[cleanName] = artist.cleanName
            it[status] = MediaStatus.AVAILABLE.name
            it[mediaSource] = MediaSource.LIDARR.name
            it[syncedAt] = Instant.now()
        }
        LibraryArtist(id, artist.foreignArtistId, artist.id, artist.artistName, artist.cleanName, MediaStatus.AVAILABLE, MediaSource.LIDARR, Instant.now())
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

    private fun mapRow(row: ResultRow) = LibraryArtist(
        id = row[LibraryArtists.id].value,
        musicbrainzId = row[LibraryArtists.musicbrainzId],
        lidarrId = row[LibraryArtists.lidarrId],
        name = row[LibraryArtists.name],
        cleanName = row[LibraryArtists.cleanName],
        status = MediaStatus.valueOf(row[LibraryArtists.status]),
        source = MediaSource.valueOf(row[LibraryArtists.mediaSource]),
        syncedAt = row[LibraryArtists.syncedAt]
    )
}
