package com.github.schaka.naviseerr.download_client.slskd

import com.github.schaka.naviseerr.config.RestClientProperties
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "clients.soulseek")
data class SoulseekProperties(
    override val url: String,
    override val apiKey: String,
) : RestClientProperties