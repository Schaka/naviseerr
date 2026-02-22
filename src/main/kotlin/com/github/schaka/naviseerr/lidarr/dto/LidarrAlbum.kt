package com.github.schaka.naviseerr.lidarr.dto

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

data class LidarrAlbum(
    @JsonSetter(nulls = Nulls.SKIP)
    val id: Long = 0,
    val artistId: Long,
    val albumType: String,
    val title: String,
    val foreignAlbumId: String,
    val path: String?,
    val monitored: Boolean = false,
    val statistics: AlbumStatistics? = null
)
