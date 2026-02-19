package com.github.schaka.naviseerr.library

import com.github.schaka.naviseerr.db.library.LibraryAlbumService
import com.github.schaka.naviseerr.db.library.LibraryArtistService
import com.github.schaka.naviseerr.lidarr.LidarrClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class LibrarySyncService(
    private val lidarrClient: LidarrClient,
    private val libraryArtistService: LibraryArtistService,
    private val libraryAlbumService: LibraryAlbumService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    fun syncLibrary() {
        try {
            log.info("Starting library sync from Lidarr")
            val artists = lidarrClient.getAllArtists()
            val allAlbums = lidarrClient.getAllAlbums()
            val albumsByArtist = allAlbums.groupBy { it.artistId }

            artists.forEach { artist ->
                val libraryArtist = libraryArtistService.upsertFromLidarr(artist)
                val albums = albumsByArtist[artist.id] ?: emptyList()
                albums.forEach { album ->
                    libraryAlbumService.upsertFromLidarr(libraryArtist.id, album)
                }
            }

            log.info("Library sync complete: {} artists, {} albums", artists.size, allAlbums.size)
        } catch (e: Exception) {
            log.error("Library sync failed", e)
        }
    }
}
