package com.github.schaka.naviseerr.navidrome.polling.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class NavidromeSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,

    val artistId: String?,
    val albumId: String?,
    val albumArtist: String?,
    val albumArtistId: String?,

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val playCount: Int = 0,
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val duration: Double = 0.0,

    val playDate: Instant? = null,

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val genres: List<NavidromeGenre> = emptyList(),

    // MusicBrainz IDs
    val mbzTrackId: String? = null,
    val mbzArtistId: String? = null,
    val mbzAlbumId: String? = null,
)

data class NavidromeGenre(
    val id: String,
    val name: String
)
