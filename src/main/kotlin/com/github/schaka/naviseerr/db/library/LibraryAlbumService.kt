package com.github.schaka.naviseerr.db.library

import com.github.schaka.naviseerr.db.library.enums.MediaSource
import com.github.schaka.naviseerr.db.library.enums.MediaStatus
import com.github.schaka.naviseerr.lidarr.dto.LidarrAlbum
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
class LibraryAlbumService {

    fun upsertFromLidarr(libraryArtistId: UUID, album: LidarrAlbum): LibraryAlbum = transaction {
        val existing = LibraryAlbums.selectAll()
            .where { LibraryAlbums.lidarrId eq album.id }
            .singleOrNull()

        if (existing != null) {
            LibraryAlbums.update({ LibraryAlbums.lidarrId eq album.id }) {
                it[title] = album.title
                it[musicbrainzId] = album.foreignAlbumId
                it[albumType] = album.albumType
                it[status] = MediaStatus.AVAILABLE.name
                it[syncedAt] = Instant.now()
            }
            return@transaction mapRow(existing).copy(
                title = album.title,
                musicbrainzId = album.foreignAlbumId,
                albumType = album.albumType,
                status = MediaStatus.AVAILABLE,
                syncedAt = Instant.now()
            )
        }

        val id = UUID.randomUUID()
        LibraryAlbums.insert {
            it[this.id] = id
            it[artistId] = libraryArtistId
            it[musicbrainzId] = album.foreignAlbumId
            it[lidarrId] = album.id
            it[title] = album.title
            it[albumType] = album.albumType
            it[status] = MediaStatus.AVAILABLE.name
            it[mediaSource] = MediaSource.LIDARR.name
            it[syncedAt] = Instant.now()
        }
        LibraryAlbum(id, libraryArtistId, album.foreignAlbumId, album.id, album.title, album.albumType, MediaStatus.AVAILABLE, MediaSource.LIDARR, Instant.now())
    }

    fun findByMusicbrainzId(mbId: String): LibraryAlbum? = transaction {
        LibraryAlbums.selectAll()
            .where { LibraryAlbums.musicbrainzId eq mbId }
            .singleOrNull()
            ?.let(::mapRow)
    }

    fun findByArtistId(artistId: UUID): List<LibraryAlbum> = transaction {
        LibraryAlbums.selectAll()
            .where { LibraryAlbums.artistId eq artistId }
            .map(::mapRow)
    }

    private fun mapRow(row: ResultRow) = LibraryAlbum(
        id = row[LibraryAlbums.id].value,
        artistId = row[LibraryAlbums.artistId].value,
        musicbrainzId = row[LibraryAlbums.musicbrainzId],
        lidarrId = row[LibraryAlbums.lidarrId],
        title = row[LibraryAlbums.title],
        albumType = row[LibraryAlbums.albumType],
        status = MediaStatus.valueOf(row[LibraryAlbums.status]),
        source = MediaSource.valueOf(row[LibraryAlbums.mediaSource]),
        syncedAt = row[LibraryAlbums.syncedAt]
    )
}
