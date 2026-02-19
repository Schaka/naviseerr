package com.github.schaka.naviseerr.library

import com.github.schaka.naviseerr.db.library.*
import com.github.schaka.naviseerr.db.library.enums.MediaStatus
import com.github.schaka.naviseerr.db.library.enums.RequestStatus
import com.github.schaka.naviseerr.db.user.NaviseerrUser
import com.github.schaka.naviseerr.lidarr.LidarrClient
import com.github.schaka.naviseerr.lidarr.LidarrConfigCache
import com.github.schaka.naviseerr.lidarr.dto.LidarrAddArtistRequest
import com.github.schaka.naviseerr.lidarr.dto.LidarrAddAlbumRequest
import com.github.schaka.naviseerr.lidarr.dto.LidarrMonitorRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RequestService(
    private val lidarrClient: LidarrClient,
    private val mediaRequestService: MediaRequestService,
    private val libraryArtistService: LibraryArtistService,
    private val libraryAlbumService: LibraryAlbumService,
    private val lidarrConfigCache: LidarrConfigCache,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun requestArtist(user: NaviseerrUser, mbArtistId: String, artistName: String): MediaRequest {
        val existingArtist = libraryArtistService.findByMusicbrainzId(mbArtistId)
        if (existingArtist?.status == MediaStatus.AVAILABLE) {
            throw MediaAlreadyAvailableException("Artist '$artistName' is already in the library")
        }

        val existingRequest = mediaRequestService.findPendingByMusicbrainzArtistId(user.id, mbArtistId)
        if (existingRequest != null) return existingRequest

        val request = mediaRequestService.create(user.id, mbArtistId, null, artistName, null)

        val lidarrLookup = lidarrClient.lookupArtist("lidarr:$mbArtistId")
        val config = lidarrConfigCache.getConfig()

        val lidarrArtistId = if (lidarrLookup.isNotEmpty() && lidarrLookup.first().id != 0L) {
            lidarrLookup.first().id
        } else {
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
            added.id
        }

        return mediaRequestService.updateStatus(request.id, RequestStatus.PROCESSING, lidarrArtistId = lidarrArtistId)
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

        val existingRequest = mediaRequestService.findPendingByMusicbrainzAlbumId(user.id, mbAlbumId)
        if (existingRequest != null) return existingRequest

        val request = mediaRequestService.create(user.id, mbArtistId, mbAlbumId, artistName, albumTitle)

        val config = lidarrConfigCache.getConfig()

        val artistLookup = lidarrClient.lookupArtist("lidarr:$mbArtistId")
        val lidarrArtistId = if (artistLookup.isNotEmpty()) {
            artistLookup.first().id
        } else {
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
            added.id
        }

        val albumLookup = lidarrClient.lookupAlbum("lidarr:$mbAlbumId")
        val lidarrAlbumId = if (albumLookup.isNotEmpty()) {
            val album = albumLookup.first()
            lidarrClient.monitorAlbums(LidarrMonitorRequest(listOf(album.id)))
            album.id
        } else {
            val added = lidarrClient.addAlbum(
                LidarrAddAlbumRequest(
                    foreignAlbumId = mbAlbumId,
                    title = albumTitle,
                    artistId = lidarrArtistId,
                )
            )
            log.info("Added album '{}' to Lidarr with id {}", albumTitle, added.id)
            added.id
        }

        return mediaRequestService.updateStatus(request.id, RequestStatus.PROCESSING, lidarrArtistId = lidarrArtistId, lidarrAlbumId = lidarrAlbumId)
    }
}
