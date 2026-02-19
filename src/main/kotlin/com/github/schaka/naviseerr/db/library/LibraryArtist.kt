package com.github.schaka.naviseerr.db.library

import com.github.schaka.naviseerr.db.library.enums.MediaSource
import com.github.schaka.naviseerr.db.library.enums.MediaStatus
import java.time.Instant
import java.util.UUID

data class LibraryArtist(
    val id: UUID,
    val musicbrainzId: String?,
    val lidarrId: Long?,
    val name: String,
    val cleanName: String?,
    val status: MediaStatus,
    val source: MediaSource,
    val syncedAt: Instant
)
