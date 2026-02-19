package com.github.schaka.naviseerr.lidarr.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class LidarrMetadataProfile(
    val id: Int,
    val name: String
)
