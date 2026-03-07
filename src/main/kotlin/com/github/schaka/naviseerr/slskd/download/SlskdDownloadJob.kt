package com.github.schaka.naviseerr.slskd.download

import com.github.schaka.naviseerr.db.download.enums.DownloadJobStatus
import java.time.Instant
import java.util.UUID

data class SlskdDownloadJob(
    val id: UUID,
    val status: DownloadJobStatus,
    val artistName: String,
    val albumTitle: String?,
    val musicbrainzArtistId: String?,
    val musicbrainzAlbumId: String?,
    val slskdUsername: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
