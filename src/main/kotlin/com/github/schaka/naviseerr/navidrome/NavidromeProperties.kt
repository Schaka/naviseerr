package com.github.schaka.naviseerr.navidrome

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "clients.navidrome")
data class NavidromeProperties(
        val url: String,
        val username: String,
        val password: String,
)