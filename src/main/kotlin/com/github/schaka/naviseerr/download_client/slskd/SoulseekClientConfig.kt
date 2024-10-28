package com.github.schaka.naviseerr.download_client.slskd

import com.fasterxml.jackson.databind.ObjectMapper
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Configuration(proxyBeanMethods = false)
class SoulseekClientConfig {

    @Bean
    fun soulseekClient(properties: SoulseekProperties, mapper: ObjectMapper): SoulseekClient {
        return Feign.builder()
            .decoder(JacksonDecoder(mapper))
            .encoder(JacksonEncoder(mapper))
            .requestInterceptor {
                it.header("X-API-Key", properties.apiKey)
                it.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            }
            .target(SoulseekClient::class.java, "${properties.url}/api/v0")
    }
}