package com.github.schaka.naviseerr.listenbrainz

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "listenbrainz")
class ListenBrainzProperties(
    val url: String = "https://api.listenbrainz.org/1"
)
