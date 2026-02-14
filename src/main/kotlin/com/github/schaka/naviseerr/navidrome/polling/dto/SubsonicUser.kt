package com.github.schaka.naviseerr.navidrome.polling.dto

data class SubsonicUserResponse(
    val subsonicResponse: SubsonicUserBody
)

data class SubsonicUserBody(
    val status: String,
    val version: String,
    val user: SubsonicUser?
)

data class SubsonicUser(
    val username: String,
    val email: String?,
    val scrobblingEnabled: Boolean,
    val adminRole: Boolean,
    val settingsRole: Boolean?,
    val downloadRole: Boolean?,
    val uploadRole: Boolean?,
    val playlistRole: Boolean?,
    val coverArtRole: Boolean?,
    val commentRole: Boolean?,
    val podcastRole: Boolean?,
    val streamRole: Boolean?,
    val jukeboxRole: Boolean?,
    val shareRole: Boolean?
)
