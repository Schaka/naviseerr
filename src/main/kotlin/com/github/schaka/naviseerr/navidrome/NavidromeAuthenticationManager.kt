package com.github.schaka.naviseerr.navidrome

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class NavidromeAuthenticationManager(
    private val navidromeProperties: NavidromeProperties,
    private val objectMapper: ObjectMapper
) : AuthenticationManager {

    override fun authenticate(authentication: Authentication?): Authentication? {
        val userProperties = navidromeProperties.copy(
            username = authentication?.principal.toString(),
            password = authentication?.credentials.toString()
        )

        try {
            val client = newNavidromeClient(userProperties, objectMapper)
            val user = client.listUsers()
        } catch (e: Exception) {
            throw BadCredentialsException("Couldn't log in to navidrome client", e)
        }

        return authentication
    }
}