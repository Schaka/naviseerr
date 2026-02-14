package com.github.schaka.naviseerr.navidrome.polling.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

data class SubsonicNowPlayingResponse(

    @JsonProperty("subsonic-response")
    val subsonicResponse: SubsonicNowPlayingBody
)

data class SubsonicNowPlayingBody(
    val status: String,
    val version: String,
    val nowPlaying: SubsonicNowPlaying?
)

data class SubsonicNowPlaying(
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val entry: List<SubsonicNowPlayingEntry> = emptyList()
)

data class SubsonicNowPlayingEntry(
    val id: String,
    val parent: String?,
    val title: String,
    val album: String?,
    val artist: String,
    val isDir: Boolean = false,
    val coverArt: String?,
    val created: String,
    val duration: Int,
    val bitRate: Int?,
    val track: Int?,
    val year: Int?,
    val genre: String?,
    val size: Long,
    val suffix: String?,
    val contentType: String?,
    val albumId: String?,
    val artistId: String?,
    val type: String?,
    val username: String,
    val minutesAgo: Int,
    val playerId: Int,
    val playerName: String?
)
