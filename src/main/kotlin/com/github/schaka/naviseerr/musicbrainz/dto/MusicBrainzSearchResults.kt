package com.github.schaka.naviseerr.musicbrainz.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty


@JsonIgnoreProperties(ignoreUnknown = true)
data class MusicBrainzArtistSearchResult(
    val created: String?,
    val count: Int,
    val offset: Int,
    val artists: List<MusicBrainzArtist>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MusicBrainzReleaseGroupSearchResult(
    val created: String?,
    val count: Int,
    val offset: Int,
    @JsonProperty("release-groups")
    val releaseGroups: List<MusicBrainzReleaseGroup>
)
