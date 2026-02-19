package com.github.schaka.naviseerr.db.library

import com.github.schaka.naviseerr.db.library.enums.RequestStatus
import java.time.Instant
import java.util.UUID

data class MediaRequest(
    val id: UUID,
    val userId: UUID,
    val artistId: UUID?,
    val albumId: UUID?,
    val musicbrainzArtistId: String,
    val musicbrainzAlbumId: String?,
    val artistName: String,
    val albumTitle: String?,
    val status: RequestStatus,
    val lidarrArtistId: Long?,
    val lidarrAlbumId: Long?,
    val createdAt: Instant,
    val updatedAt: Instant
)
