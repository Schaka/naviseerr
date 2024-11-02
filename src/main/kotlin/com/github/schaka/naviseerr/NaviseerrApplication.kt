package com.github.schaka.naviseerr

import com.github.schaka.janitorr.mediaserver.NavidromeClient
import com.github.schaka.naviseerr.config.RestClientProperties
import com.github.schaka.naviseerr.download_client.slskd.SoulseekClient
import com.github.schaka.naviseerr.music_library.lidarr.LidarrClient
import org.flywaydb.core.internal.configuration.extensions.DeployScriptFilenameConfigurationExtension
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
            hints.proxies().registerJdkProxy(LidarrClient::class.java)
            hints.proxies().registerJdkProxy(RestClientProperties::class.java)
            hints.proxies().registerJdkProxy(SoulseekClient::class.java)
            hints.proxies().registerJdkProxy(NavidromeClient::class.java)

            hints.resources().registerPattern("com.github.schaka/*")
        }
    }
}

fun main(args: Array<String>) {
    runApplication<NaviseerrApplication>(*args)
}

