package com.github.schaka.naviseerr.lidarr.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

@JsonIgnoreProperties(ignoreUnknown = true)
data class LidarrMetadataProfile(
    val id: Int,
    val name: String,

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val primarilyAlbumTypes: List<LidarrMetadataAlbumSetting>,
)
