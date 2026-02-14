package com.github.schaka.naviseerr.slskd

import com.github.schaka.naviseerr.config.DefaultClientProperties
import feign.Feign
import feign.Request
import feign.jackson3.Jackson3Decoder
import feign.jackson3.Jackson3Encoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import tools.jackson.databind.json.JsonMapper

@Configuration
class SoulseekClientConfig(
    val properties: SoulseekProperties,
    val defaults: DefaultClientProperties,
    val mapper: JsonMapper
) {

    @Bean
    fun soulseekClient(): SoulseekClient {
        return Feign.builder()
            .options(Request.Options(defaults.connectTimeout, defaults.readTimeout, true))
            .decoder(Jackson3Decoder(mapper))
            .encoder(Jackson3Encoder(mapper))
            .requestInterceptor {
                it.header("X-API-Key", properties.apiKey)
                it.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            }
            .target(SoulseekClient::class.java, "${properties.url}/api/v0")
    }
}