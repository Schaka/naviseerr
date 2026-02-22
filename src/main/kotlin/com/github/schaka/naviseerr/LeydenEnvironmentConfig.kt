package com.github.schaka.naviseerr

import com.github.schaka.naviseerr.lidarr.LidarrProperties
import com.github.schaka.naviseerr.lidarr.setup.LidarrSetup
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("leyden")
class LeydenEnvironmentConfig(private val lidarrProperties: LidarrProperties) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun setupLidarr() {
        log.info("Leyden profile active — configuring Lidarr at {}", lidarrProperties.url)
        val setup = LidarrSetup(lidarrProperties.url, lidarrProperties.apiKey)
        setup.setupRootFolder()
    }
}