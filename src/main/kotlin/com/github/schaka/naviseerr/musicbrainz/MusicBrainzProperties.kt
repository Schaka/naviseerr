package com.github.schaka.naviseerr.musicbrainz

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "musicbrainz")
data class MusicBrainzProperties(
    val url: String = "https://musicbrainz.org/ws/2",
)
