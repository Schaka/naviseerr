package com.github.schaka.naviseerr.lidarr.dto

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

data class LidarrArtist(

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val id: Long,
    val artistName: String,
    val cleanName: String,
    val mbId: String?, // musicbrainz
    val foreignArtistId: String, // musicbrainz
    val path: String?,

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val albums: MutableList<LidarrAlbum> = ArrayList(),
)
