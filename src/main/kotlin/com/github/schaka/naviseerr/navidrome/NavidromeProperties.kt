package com.github.schaka.naviseerr.navidrome

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "clients.navidrome")
data class NavidromeProperties(
        val enabled: Boolean,
        val url: String,
        val username: String,
        val password: String,
)