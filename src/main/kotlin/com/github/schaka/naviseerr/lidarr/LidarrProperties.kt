package com.github.schaka.naviseerr.lidarr

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "lidarr")
data class LidarrProperties(
    val url: String,
    val apiKey: String,
    val qualityProfileId: Int = 2,
    val metadataProfileId: Int = 1,
)