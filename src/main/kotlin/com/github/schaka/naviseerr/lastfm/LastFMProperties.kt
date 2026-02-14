package com.github.schaka.naviseerr.lastfm

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "lastfm")
class LastFMProperties(
    val apiKey: String,
    val sharedSecret: String,
    val url: String = "http://ws.audioscrobbler.com/2.0"
)
