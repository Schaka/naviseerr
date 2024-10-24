package com.github.schaka.naviseerr.music_library.library

import com.github.schaka.naviseerr.music_library.library.artist.ArtistRepository
import com.github.schaka.naviseerr.music_library.library.artist.ReleaseRepository
import com.github.schaka.naviseerr.music_library.lidarr.LidarrRestService
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LibraryManager(
    val lidarrRestService: LidarrRestService,
    val artistRepository: ArtistRepository,
    val releaseRepository: ReleaseRepository,
    val create: DSLContext
) {

    @Transactional
    fun updateLibrary() {
        // build internal library from Lidarr
        val artists = lidarrRestService.getLibrary()

        for (artist in artists) {

            val artistId = artistRepository.saveArtist(artist)
            releaseRepository.saveReleases(artist.albums, artistId)
        }
    }
}