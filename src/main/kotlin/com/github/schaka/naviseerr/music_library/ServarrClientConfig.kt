package com.github.schaka.naviseerr.music_library

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.schaka.naviseerr.music_library.lidarr.LidarrClient
import com.github.schaka.naviseerr.music_library.lidarr.LidarrProperties
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Configuration(proxyBeanMethods = false)
class ServarrClientConfig {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @Bean
    fun lidarrClient(properties: LidarrProperties, mapper: ObjectMapper): LidarrClient {
        return Feign.builder()
                .decoder(JacksonDecoder(mapper))
                .encoder(JacksonEncoder(mapper))
                .requestInterceptor {
                    it.header("X-Api-Key", properties.apiKey)
                    it.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                }
                .target(LidarrClient::class.java, "${properties.url}/api/v1")
    }

}