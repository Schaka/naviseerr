package com.github.schaka.naviseerr.download.monitor

import com.github.schaka.naviseerr.db.download.DownloadJobService
import com.github.schaka.naviseerr.db.download.enums.DownloadProtocol
import com.github.schaka.naviseerr.db.library.MediaRequest
import com.github.schaka.naviseerr.db.library.MediaRequestService
import com.github.schaka.naviseerr.lidarr.LidarrClient
import com.github.schaka.naviseerr.lidarr.dto.LidarrDownloadClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class LidarrDownloadMonitor(
    private val lidarrClient: LidarrClient,
    private val mediaRequestService: MediaRequestService,
    private val downloadJobService: DownloadJobService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.MINUTES)
    fun checkLidarrHistory() {
        val downloadClients = lidarrClient.getDownloadClients()
        val knownHistoryIds = downloadJobService.findAllLidarrHistoryIds()

        mediaRequestService.findLidarrRequests()
            .forEach { processRequest(it, downloadClients, knownHistoryIds) }
    }

    private fun processRequest(
        request: MediaRequest,
        downloadClients: List<LidarrDownloadClient>,
        knownHistoryIds: Set<Long>
    ) {
        val history = lidarrClient.getArtistHistory(request.lidarrArtistId!!)

        val relevantHistory = if (request.lidarrAlbumId != null) {
            history.filter { it.albumId == request.lidarrAlbumId }
        } else {
            history
        }

        relevantHistory
            .filter { it.downloadId != null }
            .groupBy { it.downloadId!! }
            .forEach { (_, group) ->
                val importedRecord = group.find { it.eventType == "downloadImported" } ?: return@forEach

                if (importedRecord.id in knownHistoryIds) {
                    return@forEach
                }

                val clientName = group
                    .filter { it.eventType == "trackFileImported" }
                    .firstNotNullOfOrNull { it.data.downloadClient }
                val protocol = resolveProtocol(clientName, downloadClients)

                val filePaths = lidarrClient.getTrackFiles(importedRecord.albumId).map { it.path }

                log.info(
                    "Creating download job for request {} album {} history record {} (protocol: {})",
                    request.id, importedRecord.albumId, importedRecord.id, protocol
                )
                downloadJobService.createLidarrJob(
                    mediaRequestId = request.id,
                    artistName = request.artistName,
                    albumTitle = request.albumTitle,
                    mbArtistId = request.musicbrainzArtistId,
                    mbAlbumId = request.musicbrainzAlbumId,
                    lidarrArtistId = request.lidarrArtistId,
                    lidarrAlbumId = importedRecord.albumId,
                    lidarrHistoryId = importedRecord.id,
                    downloadClient = clientName,
                    protocol = protocol,
                    filePaths = filePaths
                )
            }
    }

    private fun resolveProtocol(clientName: String?, downloadClients: List<LidarrDownloadClient>): DownloadProtocol {
        val client = downloadClients.find { it.name == clientName } ?: return DownloadProtocol.UNKNOWN
        return when {
            client.implementation == "SoulseekDownloadClient" -> DownloadProtocol.SOULSEEK
            client.protocol == "torrent" -> DownloadProtocol.TORRENT
            client.protocol == "usenet" -> DownloadProtocol.USENET
            else -> DownloadProtocol.UNKNOWN
        }
    }
}
