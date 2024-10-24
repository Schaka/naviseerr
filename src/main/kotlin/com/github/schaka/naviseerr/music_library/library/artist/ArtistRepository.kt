package com.github.schaka.naviseerr.music_library.library.artist

import com.github.schaka.naviseerr.db.tables.Artists.ARTISTS
import com.github.schaka.naviseerr.music_library.library.LibraryItemState
import com.github.schaka.naviseerr.music_library.library.LibraryItemState.CHANGED
import com.github.schaka.naviseerr.music_library.library.LibraryItemState.MISSING
import com.github.schaka.naviseerr.music_library.library.LibraryItemState.UNCHANGED
import com.github.schaka.naviseerr.music_library.lidarr.dto.LidarrArtist
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class ArtistRepository(
    val create: DSLContext
) {

    fun saveArtist(artist: LidarrArtist): Long {
        val libraryArtist = convert(artist)
        if (libraryArtist.state == UNCHANGED) {
            return libraryArtist.id!!
        }

        if (libraryArtist.state == CHANGED) {
            create.update(ARTISTS)
                .set(ARTISTS.HASH, artist.hashCode())
                .set(ARTISTS.NAME, artist.artistName)
                .set(ARTISTS.MUSICBRAINZ_ID, artist.foreignArtistId)
                .set(ARTISTS.PATH, artist.path)
                .where(ARTISTS.LIDARR_ID.eq(artist.id))
                .execute()
            return libraryArtist.id!!
        }

        val artistId = create
            .insertInto(ARTISTS, ARTISTS.LIDARR_ID, ARTISTS.HASH, ARTISTS.NAME, ARTISTS.MUSICBRAINZ_ID, ARTISTS.PATH)
            .values(artist.id, artist.hashCode(), artist.artistName, artist.foreignArtistId, artist.path)
            .returningResult(ARTISTS.ID)
            .fetchOne()
            ?.into(Long::class.java)

        return artistId!!

    }

    private fun convert(artist: LidarrArtist): LibraryArtist {
        val stateAndId = getState(artist)
        return convert(artist, stateAndId.component2(), stateAndId.component1())
    }

    private fun getState(artist: LidarrArtist): Pair<LibraryItemState, Long?> {
        val existingEntry = create.select(ARTISTS.ID, ARTISTS.HASH)
            .from(ARTISTS)
            .where(ARTISTS.LIDARR_ID.eq(artist.id))
            .fetchOne()

        val state = if (existingEntry == null) MISSING else CHANGED

        if (existingEntry?.component2() == artist.hashCode()) {
            return Pair(CHANGED, existingEntry.component1())
        }

        return Pair(state, existingEntry?.component1())
    }

    private fun convert(artist: LidarrArtist, id: Long? = null, state: LibraryItemState = MISSING): LibraryArtist {
        return LibraryArtist(
            id,
            artist.id,
            artist.hashCode(),
            artist.artistName,
            artist.foreignArtistId,
            artist.path,
            state
        )
    }
}