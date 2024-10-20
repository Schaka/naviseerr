package com.github.schaka.naviseerr.servarr.radarr

import com.github.schaka.naviseerr.servarr.HistorySort
import com.github.schaka.naviseerr.servarr.ServarrProperties
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "clients.radarr")
data class RadarrProperties(
    override val enabled: Boolean,
    override val url: String,
    override val apiKey: String,
    override val determineAgeBy: HistorySort? = null,
    val onlyDeleteFiles: Boolean = false,
) : ServarrProperties