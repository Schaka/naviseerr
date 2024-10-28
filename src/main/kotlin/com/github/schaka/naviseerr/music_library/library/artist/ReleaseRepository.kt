package com.github.schaka.naviseerr.music_library.library.artist

import com.github.schaka.naviseerr.db.Tables.RELEASES
import com.github.schaka.naviseerr.music_library.library.LibraryItemState
import com.github.schaka.naviseerr.music_library.library.LibraryItemState.CHANGED
import com.github.schaka.naviseerr.music_library.library.LibraryItemState.MISSING
import com.github.schaka.naviseerr.music_library.library.LibraryItemState.UNCHANGED
import com.github.schaka.naviseerr.music_library.library.release.LibraryRelease
import com.github.schaka.naviseerr.music_library.lidarr.dto.LidarrAlbum
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class ReleaseRepository(
    val create: DSLContext
) {

    private val log = KotlinLogging.logger {}

    fun saveReleases(albums: List<LidarrAlbum>, artistId: Long): List<LibraryRelease> {

        val targetReleases = albums.map(this::convert)
        val groupedReleases = targetReleases
            .groupBy(LibraryRelease::state)

        for (entry in groupedReleases.entries) {
            if (entry.key == UNCHANGED) {
                entry.value.forEach {
                    log.trace { "Albums ${it.name } by artist: $artistId unchanged - do nothing" }
                }
                continue
            }

            if (entry.key == MISSING) {
                insertValues(entry.value, artistId)
            }

            if (entry.key == CHANGED) {
                updateValues(entry.value)
            }
        }

        return targetReleases
    }

    fun saveRelease(album: LidarrAlbum, artistId: Long): LibraryRelease {
        val libraryAlbum = convert(album)
        if (libraryAlbum.state == UNCHANGED) {
            return libraryAlbum
        }

        if (libraryAlbum.state == CHANGED) {
            updateValues(listOf(libraryAlbum))
        }

        insertValues(listOf(libraryAlbum), artistId)
        return libraryAlbum
    }

    private fun updateValues(albums: List<LibraryRelease>) {
        create.batched {
            for (album in albums) {
                it.dsl()
                    .update(RELEASES)
                    .set(RELEASES.LIDARR_ID, album.lidarrId)
                    .set(RELEASES.HASH, album.hash)
                    .set(RELEASES.NAME, album.name)
                    .set(RELEASES.MUSICBRAINZ_ID, album.musicbrainzId)
                    .set(RELEASES.PATH, album.path)
                    .set(RELEASES.TYPE, album.type)
                    .where(RELEASES.ID.eq(album.id))
                    .execute()

                log.info { "Album changed ${album.name} (${album.lidarrId}) - updating" }
            }
        }
    }

    private fun insertValues(albums: List<LibraryRelease>, artistId: Long) {
        val query = create.insertInto(RELEASES, RELEASES.ARTIST_ID, RELEASES.HASH, RELEASES.LIDARR_ID, RELEASES.NAME, RELEASES.MUSICBRAINZ_ID, RELEASES.PATH, RELEASES.TYPE)
        for (album in albums) {
            query.values(artistId, album.hash, album.lidarrId, album.name, album.musicbrainzId, album.path, album.type)
            log.info { "New album found ${album.name} (${album.lidarrId}) by artist $artistId - inserting" }
        }
        query.execute()
    }

    private fun convert(album: LidarrAlbum): LibraryRelease {
        val stateAndId = getState(album)
        return convert(album, stateAndId.component2(), stateAndId.component1())
    }

    private fun getState(album: LidarrAlbum): Pair<LibraryItemState, Long?> {
        val existingEntry = create.select(RELEASES.ID, RELEASES.HASH)
            .from(RELEASES)
            .where(RELEASES.LIDARR_ID.eq(album.id))
            .fetchOne()

        val state = if (existingEntry == null) MISSING else CHANGED

        if (existingEntry?.component2() == album.hashCode()) {
            return Pair(UNCHANGED, existingEntry.component1())
        }

        return Pair(state, existingEntry?.component1())
    }

    private fun convert(album: LidarrAlbum, id: Long? = null, state: LibraryItemState = MISSING): LibraryRelease {
        return LibraryRelease(
            id,
            album.id,
            album.hashCode(),
            album.title,
            album.foreignAlbumId,
            album.albumType,
            album.path!!,
            state
        )
    }
}