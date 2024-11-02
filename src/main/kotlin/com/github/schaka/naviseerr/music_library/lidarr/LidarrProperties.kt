package com.github.schaka.naviseerr.music_library.lidarr

import com.github.schaka.naviseerr.config.RestClientProperties
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "clients.lidarr")
data class LidarrProperties(
    override val url: String,
    override val apiKey: String,
    val isDocker : Boolean = true,
) : RestClientProperties