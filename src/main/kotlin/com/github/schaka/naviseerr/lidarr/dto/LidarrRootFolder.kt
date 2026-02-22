package com.github.schaka.naviseerr.lidarr.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class LidarrRootFolder(
    val id: Long,
    val path: String,
    val accessible: Boolean
)
