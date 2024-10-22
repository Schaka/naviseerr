package com.github.schaka.naviseerr.music_library.library

import com.github.schaka.naviseerr.db.tables.Artists.ARTISTS
import com.github.schaka.naviseerr.music_library.lidarr.LidarrRestService
import org.jooq.DSLContext
import org.springframework.stereotype.Component

@Component
class LibraryManager(
    val lidarrRestService: LidarrRestService,
    val create: DSLContext
) {

    fun doStuff() {
        // build internal library from Lidarr
        val artists = lidarrRestService.getLibrary()
        create
            .insertInto(ARTISTS, ARTISTS.LIDARR_ID, ARTISTS.NAME, ARTISTS.MUSICBRAINZ_ID, ARTISTS.PATH)
    }
}