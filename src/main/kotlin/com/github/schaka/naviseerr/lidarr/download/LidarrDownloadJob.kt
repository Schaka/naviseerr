package com.github.schaka.naviseerr.lidarr.download

import com.github.schaka.naviseerr.db.download.enums.DownloadJobStatus
import com.github.schaka.naviseerr.db.download.enums.DownloadProtocol
import java.time.Instant
import java.util.UUID

data class LidarrDownloadJob(
    val id: UUID,
    val status: DownloadJobStatus,
    val artistName: String,
    val albumTitle: String?,
    val musicbrainzArtistId: String?,
    val musicbrainzAlbumId: String?,
    val lidarrArtistId: Long,
    val lidarrAlbumId: Long,
    val lidarrHistoryId: Long,
    val downloadClient: String?,
    val downloadProtocol: DownloadProtocol,
    val createdAt: Instant,
    val updatedAt: Instant
)
