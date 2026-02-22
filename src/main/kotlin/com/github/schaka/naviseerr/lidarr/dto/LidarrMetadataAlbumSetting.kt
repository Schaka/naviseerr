package com.github.schaka.naviseerr.lidarr.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

@JsonIgnoreProperties(ignoreUnknown = true)
data class LidarrMetadataAlbumSetting(
    val albumType: LidarrAlbumType,
    val allowed: Boolean = false,

)
