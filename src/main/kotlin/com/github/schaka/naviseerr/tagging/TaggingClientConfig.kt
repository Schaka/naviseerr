package com.github.schaka.naviseerr.tagging

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
class TaggingClientConfig(
    val properties: TaggingProperties,
    val defaults: DefaultClientProperties,
    val mapper: JsonMapper
) {

    @Bean
    fun taggingClient(): TaggingClient {
        return Feign.builder()
            .options(Request.Options(defaults.connectTimeout, defaults.readTimeout, true))
            .decoder(Jackson3Decoder(mapper))
            .encoder(Jackson3Encoder(mapper))
            .requestInterceptor { it.header(CONTENT_TYPE, APPLICATION_JSON_VALUE) }
            .target(TaggingClient::class.java, properties.url)
    }
}
