package com.github.schaka.naviseerr.user.dto.settings

data class UserSettingsDto(
    val username: String,
    val lastFmSessionKey: String? = null,
    val listenBrainzToken: String? = null,
    val lastFmScrobblingEnabled: Boolean = false,
    val listenBrainzScrobblingEnabled: Boolean = false
)