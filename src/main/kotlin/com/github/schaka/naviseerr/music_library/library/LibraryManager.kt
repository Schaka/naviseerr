package com.github.schaka.naviseerr.music_library.library

import com.github.schaka.naviseerr.db.Tables.RELEASES
import com.github.schaka.naviseerr.db.tables.Artists.ARTISTS
import com.github.schaka.naviseerr.music_library.lidarr.LidarrRestService
import com.github.schaka.naviseerr.music_library.lidarr.dto.Artist
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LibraryManager(
    val lidarrRestService: LidarrRestService,
    val create: DSLContext
) {

    @Transactional
    fun updateLibrary() {
        // build internal library from Lidarr
        val artists = lidarrRestService.getLibrary()

        for (artist in artists) {

            if ( exists(artist) ) {
                // FIXME: UPDATE HERE
                continue
            }

            val artistId = create
                .insertInto(ARTISTS, ARTISTS.LIDARR_ID, ARTISTS.NAME, ARTISTS.MUSICBRAINZ_ID, ARTISTS.PATH)
                .values(artist.id, artist.artistName, artist.foreignArtistId, artist.path)
                .returningResult(ARTISTS.ID)
                .fetchOne()
                ?.into(Long::class.java)

            val albumQuery = create.insertInto(RELEASES, RELEASES.ARTIST_ID, RELEASES.LIDARR_ID, RELEASES.NAME, RELEASES.MUSICBRAINZ_ID, RELEASES.PATH, RELEASES.TYPE)
            for (album in artist.albums) {
                albumQuery.values(artistId, album.id, album.title, album.foreignAlbumId, album.path, album.albumType)
            }
            albumQuery.execute()
        }
    }

    private fun exists(artist: Artist): Boolean {
        val existingId = create.select(ARTISTS.ID)
            .from(ARTISTS)
            .where(ARTISTS.LIDARR_ID.eq(artist.id))
            .fetchOne()

        return existingId != null
    }
}