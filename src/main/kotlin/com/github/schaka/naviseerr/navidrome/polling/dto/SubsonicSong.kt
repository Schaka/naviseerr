package com.github.schaka.naviseerr.navidrome.polling.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SubsonicSongResponse(

    @JsonProperty("subsonic-response")
    val subsonicResponse: SubsonicSongBody,

)

data class SubsonicSongBody(
    val status: String,
    val version: String,
    val song: SubsonicSong?,

    val code: Int?,
    val message: String?
)

data class SubsonicSong(
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
    val musicBrainzId: String?,
    val path: String?
)
