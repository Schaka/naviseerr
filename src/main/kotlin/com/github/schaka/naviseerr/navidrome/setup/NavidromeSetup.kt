package com.github.schaka.naviseerr.navidrome.setup

import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

class NavidromeSetup(private val baseUrl: String) {

    private val client = RestClient.create()

    /**
     * Creates the initial admin user on first run via Navidrome's bootstrap endpoint.
     * The endpoint is only accessible when no users exist, so subsequent runs fail gracefully.
     */
    fun createAdmin() {
        try {
            client.post()
                .uri("$baseUrl/auth/createAdmin")
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("username" to "admin", "password" to "admin"))
                .retrieve()
                .toBodilessEntity()
        } catch (e: Exception) {
            // Admin already exists from a previous run — safe to ignore
        }
    }
}
