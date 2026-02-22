package com.github.schaka.naviseerr.musicbrainz.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class MusicBrainzArtist(
    val id: String,
    val name: String,
    @JsonProperty("sort-name")
    val sortName: String?,
    val disambiguation: String?,
    val type: String?,
    val country: String?,
    val tags: List<MusicBrainzTag>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MusicBrainzTag(
    val name: String,
    val count: Int
)
