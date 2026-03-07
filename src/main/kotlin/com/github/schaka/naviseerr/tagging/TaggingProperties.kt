package com.github.schaka.naviseerr.tagging

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tagging")
data class TaggingProperties(
    val url: String
)
