package com.github.schaka.naviseerr.lidarr

import feign.Feign
import feign.RequestTemplate
import feign.codec.Encoder
import feign.jackson3.Jackson3Decoder
import feign.jackson3.Jackson3Encoder
import org.apache.http.HttpHeaders.CONTENT_TYPE
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import tools.jackson.databind.json.JsonMapper
import java.lang.reflect.Type

@Configuration
class ServarrClientConfig(
    private val properties: LidarrProperties,
    private val mapper: JsonMapper,
) {

    @Bean
    fun lidarrClient(): LidarrClient {
        return Feign.builder()
            .decoder(Jackson3Decoder(mapper))
            .encoder(PageableQueryEncoder(Jackson3Encoder(mapper)))
            .requestInterceptor {
                it.header("X-Api-Key", properties.apiKey)
                it.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            }
            .target(LidarrClient::class.java, "${properties.url}/api/v1")
    }

    // https://github.com/spring-cloud/spring-cloud-netflix/issues/556#issuecomment-254144593
    private class PageableQueryEncoder(
        private val delegate: Encoder
    ): Encoder {
        override fun encode(
            payload: Any?,
            bodyType: Type?,
            template: RequestTemplate
        ) {
            if (payload is Pageable) {
                template.query("page", "${payload.pageNumber + 1}")
                template.query("pageSize", "${payload.pageSize}")
                return
            }

            delegate.encode(payload, bodyType, template)
        }

    }

}