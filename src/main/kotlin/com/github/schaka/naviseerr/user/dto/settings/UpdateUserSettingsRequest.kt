package com.github.schaka.naviseerr.user.dto.settings

data class UpdateUserSettingsRequest(
    val listenBrainzToken: String? = null,
    val lastFmScrobblingEnabled: Boolean? = null,
    val listenBrainzScrobblingEnabled: Boolean? = null
)