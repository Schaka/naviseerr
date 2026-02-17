package com.github.schaka.naviseerr.navidrome

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "navidrome")
data class NavidromeProperties (
    /** The base URL of the Navidrome instance (e.g., http://localhost:4533) */
    val url: String,

    /**
     * Admin username used for background polling of 'now playing' data.
     * Required to see all users' activities.
     */
    val adminUser: String,

    /** Admin password (or token if supported) */
    val adminPass: String
)