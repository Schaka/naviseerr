package com.github.schaka.naviseerr.db.library

import com.github.schaka.naviseerr.db.library.enums.MediaSource
import com.github.schaka.naviseerr.db.library.enums.MediaStatus
import java.time.Instant
import java.util.UUID

data class LibraryAlbum(
    val id: UUID,
    val artistId: UUID,
    val musicbrainzId: String?,
    val lidarrId: Long?,
    val title: String,
    val albumType: String?,
    val status: MediaStatus,
    val source: MediaSource,
    val syncedAt: Instant,
    val lastSearchedAt: Instant?
)
