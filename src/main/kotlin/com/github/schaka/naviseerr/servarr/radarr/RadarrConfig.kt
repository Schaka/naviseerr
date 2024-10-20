package com.github.schaka.naviseerr.servarr.radarr

import com.github.schaka.naviseerr.config.ApplicationProperties
import com.github.schaka.naviseerr.config.FileSystemProperties
import com.github.schaka.naviseerr.servarr.ServarrService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Only required for native image
 */
@Configuration(proxyBeanMethods = false)
class RadarrConfig(
    //val radarrRestService: RadarrRestService,
    val radarrNoOpService: RadarrNoOpService
) {

    @Bean
    @Radarr
    fun radarrService(
        radarrProperties: RadarrProperties,
        radarrClient: RadarrClient,
        applicationProperties: ApplicationProperties,
        fileSystemProperties: FileSystemProperties
    ): ServarrService {

        if (radarrProperties.enabled) {
            return RadarrRestService(radarrClient, applicationProperties, fileSystemProperties, radarrProperties)
            //return radarrRestService
        }

        return radarrNoOpService
    }
}