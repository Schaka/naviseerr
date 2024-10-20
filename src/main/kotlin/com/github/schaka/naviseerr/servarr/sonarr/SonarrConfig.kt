package com.github.schaka.naviseerr.servarr.sonarr

import com.github.schaka.naviseerr.config.ApplicationProperties
import com.github.schaka.naviseerr.config.FileSystemProperties
import com.github.schaka.naviseerr.servarr.ServarrService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Only required for native image
 */
@Configuration(proxyBeanMethods = false)
class SonarrConfig(
    //val sonarrRestService: SonarrRestService,
    val sonarrNoOpService: SonarrNoOpService
) {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @Bean
    @Sonarr
    fun sonarrService(
        sonarrProperties: SonarrProperties,
        sonarrClient: SonarrClient,
        filesystemProperties: FileSystemProperties,
        applicationProperties: ApplicationProperties
    ): ServarrService {

        if (sonarrProperties.enabled) {
            //return sonarrRestService
            return SonarrRestService(sonarrClient, filesystemProperties, applicationProperties, sonarrProperties)
        }

        return sonarrNoOpService
    }
}