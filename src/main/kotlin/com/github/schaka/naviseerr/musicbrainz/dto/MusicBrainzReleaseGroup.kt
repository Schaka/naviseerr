package com.github.schaka.naviseerr.musicbrainz.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class MusicBrainzReleaseGroup(
    val id: String,
    val title: String,
    @JsonProperty("primary-type")
    val primaryType: String?,
    @JsonProperty("first-release-date")
    val firstReleaseDate: String?,
    @JsonProperty("artist-credit")
    val artistCredit: List<MusicBrainzArtistCredit>?,
    val tags: List<MusicBrainzTag>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MusicBrainzArtistCredit(
    val name: String,
    val artist: MusicBrainzArtist
)
