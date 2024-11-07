package com.github.schaka.naviseerr.navidrome

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.schaka.naviseerr.mediaserver.NavidromeClient
import com.github.schaka.naviseerr.navidrome.NavidromeClientConfig.NavidromeUserInterceptor
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

data class NavidromeUserInfo(
    val id: String,
    val isAdmin: Boolean,
    val lastFMApiKey: String,
    val name: String,
    val subsonicSalt: String,
    val subsonicToken: String,
    val token: String,
    val username: String,
)

data class NavidromeSessionUser(
    val id: String,
    val username: String,
    val restClient: NavidromeClient,

    val name: String,
    val lastFMApiKey: String,
)

fun loginToNavidrome(properties: NavidromeProperties): ResponseEntity<NavidromeUserInfo> {
    val login = RestTemplate()
    val headers = HttpHeaders()
    headers.set(CONTENT_TYPE, APPLICATION_JSON_VALUE)

    val loginInfo = """
                {
                  "username": "${properties.username}",
                  "password": "${properties.password}"
                }
            """.trimIndent()

    return login.exchange("${properties.url}/auth/login", HttpMethod.POST, HttpEntity(loginInfo, headers), NavidromeUserInfo::class.java)
}

fun newNavidromeClient(properties: NavidromeProperties, mapper: ObjectMapper): NavidromeClient {
    return Feign.builder()
        .decoder(JacksonDecoder(mapper))
        .encoder(JacksonEncoder(mapper))
        .requestInterceptor(NavidromeUserInterceptor(properties))
        .target(NavidromeClient::class.java, "${properties.url}/api")
}