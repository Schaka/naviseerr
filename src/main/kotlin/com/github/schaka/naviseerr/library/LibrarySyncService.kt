package com.github.schaka.naviseerr.library

import com.github.schaka.naviseerr.db.library.LibraryAlbumService
import com.github.schaka.naviseerr.db.library.LibraryArtistService
import com.github.schaka.naviseerr.db.library.MediaRequestService
import com.github.schaka.naviseerr.db.library.enums.MediaStatus
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
    private val mediaRequestService: MediaRequestService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    fun syncLibrary() {
        log.info("Starting library sync from Lidarr")
        val artists = lidarrClient.getAllArtists()
        val allAlbums = lidarrClient.getAllAlbums()
        val albumsByArtist = allAlbums.groupBy { it.artistId }

        artists.forEach { artist ->
            try {
                val libraryArtist = libraryArtistService.upsertFromLidarr(artist, false)

                val albums = albumsByArtist[artist.id] ?: emptyList()
                val savedAlbums = albums.map { album ->
                    libraryAlbumService.upsertFromLidarr(libraryArtist.id, album)
                }

                val allAlbumsAvailable = savedAlbums.isNotEmpty() && savedAlbums.all { it.status == MediaStatus.AVAILABLE }
                val finalArtist = libraryArtistService.upsertFromLidarr(artist, allAlbumsAvailable)

                savedAlbums.filter { it.status == MediaStatus.AVAILABLE }.forEach { album ->
                    if (album.lidarrId != null) {
                        mediaRequestService.updateAllActiveToAvailableByLidarrAlbumId(album.lidarrId)
                    }
                }

                if (finalArtist.status == MediaStatus.AVAILABLE && artist.id != 0L) {
                    mediaRequestService.updateAllActiveToAvailableByLidarrArtistId(artist.id)
                }
            } catch (e: Exception) {
                log.error("Failed to sync artist '{}' (lidarrId={})", artist.artistName, artist.id, e)
            }
        }

        log.info("Library sync complete: {} artists, {} albums", artists.size, allAlbums.size)
    }
}
