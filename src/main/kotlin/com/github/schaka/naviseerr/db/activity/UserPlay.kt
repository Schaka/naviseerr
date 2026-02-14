package com.github.schaka.naviseerr.db.activity

import java.time.Instant
import java.util.UUID

data class UserPlay(
    val id: UUID,
    val userId: UUID,
    val navidromePlayId: String,
    val trackId: String,
    val trackName: String,
    val artistName: String,
    val albumName: String?,
    val duration: Int, // in seconds
    val playedAt: Instant,
    val scrobbledToLastFm: Boolean = false,
    val scrobbledToListenBrainz: Boolean = false,
    val musicBrainzTrackId: String? = null,
    val musicBrainzArtistId: String? = null,
    val musicBrainzAlbumId: String? = null,
    val createdAt: Instant = Instant.now()
)
