package com.github.schaka.naviseerr.library

import com.github.schaka.naviseerr.db.library.*
import com.github.schaka.naviseerr.db.library.MediaRequests.albumTitle
import com.github.schaka.naviseerr.db.library.MediaRequests.lidarrAlbumId
import com.github.schaka.naviseerr.db.library.enums.MediaStatus
import com.github.schaka.naviseerr.db.user.NaviseerrUser
import com.github.schaka.naviseerr.lidarr.LidarrClient
import com.github.schaka.naviseerr.lidarr.LidarrConfigCache
import com.github.schaka.naviseerr.lidarr.dto.AddArtistOptions
import com.github.schaka.naviseerr.lidarr.dto.LidarrAddArtistRequest
import com.github.schaka.naviseerr.lidarr.dto.LidarrAddAlbumRequest
import com.github.schaka.naviseerr.lidarr.dto.LidarrAlbum
import com.github.schaka.naviseerr.lidarr.dto.LidarrArtist
import com.github.schaka.naviseerr.lidarr.dto.LidarrMonitorRequest
import com.github.schaka.naviseerr.lidarr.dto.LidarrSearchCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class RequestService(
    private val lidarrClient: LidarrClient,
    private val mediaRequestService: MediaRequestService,
    private val libraryArtistService: LibraryArtistService,
    private val libraryAlbumService: LibraryAlbumService,
    private val lidarrConfigCache: LidarrConfigCache,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val searchCooldown = Duration.ofHours(12)

    fun requestArtist(user: NaviseerrUser, mbArtistId: String, artistName: String): MediaRequest {
        val existingArtist = libraryArtistService.findByMusicbrainzId(mbArtistId)

        if (existingArtist?.status == MediaStatus.AVAILABLE) {
            throw MediaAlreadyAvailableException("Artist '$artistName' is already in the library")
        }

        val lidarrLookup = lidarrClient.lookupArtist("lidarr:$mbArtistId")
        val config = lidarrConfigCache.getConfig()

        if (existingArtist == null || lidarrLookup.isEmpty() || lidarrLookup.first().id == 0L) {
            val existingRequest = mediaRequestService.findActiveByMusicbrainzArtistId(user.id, mbArtistId)
            if (existingRequest != null) return existingRequest

            val added = lidarrClient.addArtist(
                LidarrAddArtistRequest(
                    foreignArtistId = mbArtistId,
                    artistName = artistName,
                    qualityProfileId = config.qualityProfileId,
                    metadataProfileId = config.metadataProfileId,
                    rootFolderPath = config.rootFolderPath,
                )
            )
            log.info("Added artist '{}' to Lidarr with id {}", artistName, added.id)
            libraryArtistService.upsertFromLidarr(added, false, true)
            return mediaRequestService.create(user.id, mbArtistId, null, artistName, null, lidarrArtistId = added.id)
        }

        val lidarrArtist = lidarrLookup.first()
        if (existingArtist.status == MediaStatus.UNMONITORED) {
            validateSearchAllowed(existingArtist)
            return monitorAndSearchArtist(lidarrArtist, artistName, user, mbArtistId)
        }

        validateSearchAllowed(existingArtist)

        lidarrClient.searchAlbums(LidarrSearchCommand("ArtistSearch", artistId = lidarrArtist.id))
        libraryArtistService.updateAfterSearch(lidarrArtist.id, Instant.now())
        log.info("Triggered re-search for monitored artist '{}' in Lidarr", artistName)
        return mediaRequestService.create(user.id, mbArtistId, null, artistName, null, lidarrArtistId = lidarrArtist.id)
    }

    fun requestAlbum(
        user: NaviseerrUser,
        mbArtistId: String,
        mbAlbumId: String,
        artistName: String,
        albumTitle: String
    ): MediaRequest {
        val existingAlbum = libraryAlbumService.findByMusicbrainzId(mbAlbumId)

        if (existingAlbum?.status == MediaStatus.AVAILABLE) {
            throw MediaAlreadyAvailableException("Album '$albumTitle' is already in the library")
        }

        if (existingAlbum?.status == MediaStatus.MONITORED) {
            validateSearchAllowed(existingAlbum)
            return monitorAndSearchAlbum(existingAlbum, albumTitle, user, mbArtistId, mbAlbumId, artistName)
        }

        val existingRequest = mediaRequestService.findActiveByMusicbrainzAlbumId(user.id, mbAlbumId)
        if (existingRequest != null) return existingRequest

        val config = lidarrConfigCache.getConfig()
        val artistLookup = lidarrClient.lookupArtist("lidarr:$mbArtistId")
        var artistJustAdded = false

        val lidarrArtistId = if (artistLookup.isNotEmpty() && artistLookup.first().id != 0L) {
            artistLookup.first().id
        } else {
            val added = lidarrClient.addArtist(
                LidarrAddArtistRequest(
                    foreignArtistId = mbArtistId,
                    artistName = artistName,
                    qualityProfileId = config.qualityProfileId,
                    metadataProfileId = config.metadataProfileId,
                    rootFolderPath = config.rootFolderPath,
                    addOptions = AddArtistOptions("none", false)
                )
            )

            log.info("Added artist '{}' to Lidarr with id {}", artistName, added.id)
            artistJustAdded = true
            libraryArtistService.upsertFromLidarr(added, false, false)
            added.id
        }

        val albumLookup = lookupAlbumWithWaitCondition(mbAlbumId, artistJustAdded)
        val libraryArtist = libraryArtistService.findByMusicbrainzId(mbArtistId) ?: throw IllegalStateException("Artist: '$artistName' - '$mbArtistId' must exist in database")

        val lidarrAlbum = if (albumLookup != null && albumLookup.id != 0L) {
            if (!albumLookup.monitored) {
                lidarrClient.monitorAlbums(LidarrMonitorRequest(listOf(albumLookup.id)))
            }
            lidarrClient.searchAlbums(LidarrSearchCommand("AlbumSearch", albumIds = listOf(albumLookup.id)))
            albumLookup.copy(monitored = true)
        } else {
            // FIXME: Confirm that this case can actually happen when a new album comes out and Lidarr doesn't update metadata fast enough
            val added = lidarrClient.addAlbum(
                LidarrAddAlbumRequest(
                    foreignAlbumId = mbAlbumId,
                    title = albumTitle,
                    artistId = lidarrArtistId,
                )
            )
            log.info("Added album '{}' to Lidarr with id {}", albumTitle, added.id)
            added.copy(monitored = true)
        }

        libraryAlbumService.upsertFromLidarr(libraryArtist.id, lidarrAlbum, true)

        return mediaRequestService.create(
            user.id, mbArtistId, mbAlbumId, artistName, albumTitle,
            lidarrArtistId = lidarrArtistId,
            lidarrAlbumId = lidarrAlbum.id
        )
    }

    private fun monitorAndSearchAlbum(
        existingAlbum: LibraryAlbum,
        albumTitle: String,
        user: NaviseerrUser,
        mbArtistId: String,
        mbAlbumId: String,
        artistName: String
    ): MediaRequest {
        val lidarrAlbumId = existingAlbum.lidarrId!!
        lidarrClient.searchAlbums(LidarrSearchCommand("AlbumSearch", albumIds = listOf(lidarrAlbumId)))
        libraryAlbumService.updateAfterSearch(lidarrAlbumId, Instant.now())
        log.info("Triggered re-search for monitored album '{}' in Lidarr", albumTitle)
        return mediaRequestService.create(
            user.id, mbArtistId, mbAlbumId, artistName, albumTitle,
            lidarrArtistId = lidarrAlbumId, // FIXME: should be id of artist, pulled from library
            lidarrAlbumId = lidarrAlbumId
        )
    }

    private fun validateSearchAllowed(existingAlbum: LibraryAlbum) {
        val lastSearched = existingAlbum.lastSearchedAt
        if (lastSearched != null && Duration.between(lastSearched, Instant.now()) < searchCooldown) {
            throw SearchCooldownException("Album '${existingAlbum.title}' was searched recently, please wait before re-requesting")
        }
    }

    private fun monitorAndSearchArtist(
        lidarrArtist: LidarrArtist,
        artistName: String,
        user: NaviseerrUser,
        mbArtistId: String
    ): MediaRequest {
        val updatedArtist = lidarrClient.updateArtist(lidarrArtist.id, lidarrArtist.copy(monitored = true))
        val artistAlbums = lidarrClient.getAlbums(lidarrArtist.id)
        lidarrClient.monitorAlbums(LidarrMonitorRequest(artistAlbums.map { it.id }))
        lidarrClient.searchAlbums(LidarrSearchCommand("ArtistSearch", artistId = updatedArtist.id))
        libraryArtistService.updateAfterSearch(lidarrArtist.id, Instant.now())
        log.info("Re-monitoring and searching artist '{}' in Lidarr", artistName)
        return mediaRequestService.create(user.id, mbArtistId, null, artistName, null, lidarrArtistId = lidarrArtist.id)
    }

    private fun validateSearchAllowed(existingArtist: LibraryArtist) {
        val lastSearched = existingArtist.lastSearchedAt
        if (lastSearched != null && Duration.between(lastSearched, Instant.now()) < searchCooldown) {
            throw SearchCooldownException("Artist '${existingArtist.name}' was searched recently, please wait before re-requesting")
        }
    }

    /**
     * Upon adding a new artist to Lidarr, it will take a while for all albums to register. We're giving it up to 5 minutes if the artist was just added.
     */
    private fun lookupAlbumWithWaitCondition(mbAlbumId: String, artistJustAdded: Boolean, secondsWaited: Int = 0): LidarrAlbum? {
        val albumLookup = lidarrClient.lookupAlbum("lidarr:$mbAlbumId")

        if (!artistJustAdded || secondsWaited >= 300) {
            return albumLookup.firstOrNull()
        }

        if (albumLookup.isEmpty() || albumLookup.first().id == 0L) {
            Thread.sleep(5000)
            return lookupAlbumWithWaitCondition(mbAlbumId, artistJustAdded, secondsWaited + 5)
        }

        return albumLookup.firstOrNull()
    }
}
