package com.github.schaka.naviseerr.user.dto.settings

data class LastFMAuthResultDto(
    val success: Boolean,
    val message: String,
    val sessionKey: String? = null
)