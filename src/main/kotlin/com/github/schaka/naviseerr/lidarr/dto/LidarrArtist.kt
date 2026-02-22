package com.github.schaka.naviseerr.lidarr.dto

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

data class LidarrArtist(

    @JsonSetter(nulls = Nulls.SKIP)
    val id: Long = 0,
    val artistName: String,
    val cleanName: String,
    val mbId: String?, // musicbrainz
    val foreignArtistId: String, // musicbrainz
    val path: String?,
    val monitored: Boolean = false,
    val qualityProfileId: Int? = null,
    val metadataProfileId: Int? = null,

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val albums: List<LidarrAlbum> = listOf(),
)