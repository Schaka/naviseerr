package com.github.schaka.naviseerr.music_library

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.schaka.naviseerr.music_library.lidarr.LidarrClient
import com.github.schaka.naviseerr.music_library.lidarr.LidarrProperties
import feign.Feign
import feign.RequestTemplate
import feign.codec.Encoder
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import java.lang.reflect.Type

@Configuration(proxyBeanMethods = false)
class ServarrClientConfig {

    @Bean
    fun lidarrClient(properties: LidarrProperties, mapper: ObjectMapper): LidarrClient {
        return Feign.builder()
                .decoder(JacksonDecoder(mapper))
                .encoder(PageableQueryEncoder(JacksonEncoder(mapper)))
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