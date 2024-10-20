package com.github.schaka.naviseerr

import com.github.schaka.naviseerr.servarr.RestClientProperties
import com.github.schaka.naviseerr.servarr.ServarrService
import com.github.schaka.naviseerr.servarr.radarr.RadarrClient
import com.github.schaka.naviseerr.servarr.sonarr.SonarrClient
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.ImportRuntimeHints
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Path

@EnableConfigurationProperties
@EnableAsync
@EnableCaching
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
@ImportRuntimeHints(NaviseerrApplication.Hints::class)
class NaviseerrApplication {

    class Hints : RuntimeHintsRegistrar {
        override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
            hints.proxies().registerJdkProxy(RadarrClient::class.java)
            hints.proxies().registerJdkProxy(SonarrClient::class.java)
            hints.proxies().registerJdkProxy(ServarrService::class.java)
            hints.proxies().registerJdkProxy(RestClientProperties::class.java)
        }
    }
}

fun main(args: Array<String>) {
    Path.of("/home/schaka/nausicaä")
    runApplication<NaviseerrApplication>(*args)
}

