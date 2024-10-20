package com.github.schaka.naviseerr.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import java.time.Duration

@ConfigurationProperties(prefix = "application")
data class ApplicationProperties(
        val runOnce: Boolean = false,
        val dryRun: Boolean = false,
        val wholeTvShow: Boolean = false,
        val wholeShowSeedingCheck: Boolean = false,
        val leavingSoon: Duration = Duration.ofDays(14),
)