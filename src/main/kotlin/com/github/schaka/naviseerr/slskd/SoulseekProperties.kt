package com.github.schaka.naviseerr.slskd

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "slskd")
data class SoulseekProperties(
    val url: String,
    val apiKey: String,
    val downloadDir: String
)