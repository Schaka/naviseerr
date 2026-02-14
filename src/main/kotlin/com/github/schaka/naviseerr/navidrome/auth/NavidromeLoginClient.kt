package com.github.schaka.naviseerr.navidrome.auth

import com.github.schaka.naviseerr.navidrome.NavidromeProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import kotlin.jvm.java

@Service
class NavidromeLoginClient(
    private val properties: NavidromeProperties,
) {
    private val client = RestClient.builder().baseUrl(properties.url).build()

    fun authenticate(username: String, password: String): NavidromeLoginResponse {
        return client.post()
            .uri("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(NavidromeLoginRequest(username, password))
            .retrieve()
            .toEntity<NavidromeLoginResponse>(NavidromeLoginResponse::class.java)
            .body!!
    }
}

data class NavidromeLoginRequest(
    val username: String,
    val password: String
)

data class NavidromeLoginResponse(
    val id: String,
    val name: String,
    val username: String,
    val isAdmin: Boolean,
    val subsonicSalt: String,
    val subsonicToken: String,
    val token: String,
)
