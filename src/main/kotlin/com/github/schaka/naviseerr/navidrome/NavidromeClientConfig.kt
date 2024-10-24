package com.github.schaka.naviseerr.navidrome

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.schaka.janitorr.mediaserver.NavidromeClient
import feign.Feign
import feign.RequestInterceptor
import feign.RequestTemplate
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime

@Configuration(proxyBeanMethods = false)
class NavidromeClientConfig {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        private val clientId = "d4966981-a9be-4959-bf23-b803d89eebf2";
    }

    @Bean
    fun jellyfinUserClient(properties: NavidromeProperties, mapper: ObjectMapper): NavidromeClient {
        return Feign.builder()
                .decoder(JacksonDecoder(mapper))
                .encoder(JacksonEncoder(mapper))
                .requestInterceptor(NavidromeUserInterceptor(properties))
                .target(NavidromeClient::class.java, properties.url)
    }

    private class NavidromeUserInterceptor(
            val properties: NavidromeProperties
    ) : RequestInterceptor {

        var lastUpdate: LocalDateTime = LocalDateTime.MIN
        var accessToken: String = "invalid-token"

        override fun apply(template: RequestTemplate) {

            if (lastUpdate.plusMinutes(30).isBefore(LocalDateTime.now())) {
                val userInfo = getUserInfo(properties)
                accessToken = userInfo.body?.get("AccessToken").toString()
                lastUpdate = LocalDateTime.now()
                log.info("Logged in to Navidrome as {} {}", properties.username, accessToken)
            }

            template.header("X-Nd-Authorization", accessToken)
            template.header("X-Nd-Client-Unique-Id", clientId)
            template.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
        }

        private fun getUserInfo(properties: NavidromeProperties): ResponseEntity<Map<*, *>> {
            val login = RestTemplate()
            val headers = HttpHeaders()
            headers.set(CONTENT_TYPE, APPLICATION_JSON_VALUE)

            val loginInfo = """
                {
                  "username": "${properties.username}",
                  "password": "${properties.password}"
                }
            """.trimIndent()

            return login.exchange("${properties.url}/auth/login", HttpMethod.POST, HttpEntity(loginInfo, headers), Map::class.java)
        }

    }

}