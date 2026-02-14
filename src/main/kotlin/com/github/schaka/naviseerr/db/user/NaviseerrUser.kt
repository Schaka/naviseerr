package com.github.schaka.naviseerr.db.user

import java.time.Instant
import java.util.UUID

data class NaviseerrUser(
    val id: UUID,
    val username: String,
    val navidromeId: String,
    val navidromeToken: String,
    val subsonicSalt: String,
    val subsonicToken: String,
    val lastLogin: Instant = Instant.now(),
    val lastFmSessionKey: String? = null,
    val listenBrainzToken: String? = null,
    val lastFmScrobblingEnabled: Boolean = true,
    val listenBrainzScrobblingEnabled: Boolean = true
)