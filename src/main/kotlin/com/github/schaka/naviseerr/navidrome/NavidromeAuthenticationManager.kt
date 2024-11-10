package com.github.schaka.naviseerr.navidrome

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.schaka.naviseerr.user.UserRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component

@Component
class NavidromeAuthenticationManager(
    private val navidromeProperties: NavidromeProperties,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper
) : AuthenticationManager {

    override fun authenticate(authentication: Authentication?): Authentication? {
        val userProperties = navidromeProperties.copy(
            username = authentication?.principal.toString(),
            password = authentication?.credentials.toString()
        )

        try {
            val client = loginToNavidrome(userProperties).body!!
            var roles = mutableListOf(SimpleGrantedAuthority("ROLE_USER"))
            if (client.isAdmin) {
                roles.add(SimpleGrantedAuthority("ROLE_ADMIN"))
            }
            val userClient = newNavidromeClient(userProperties, objectMapper)
            val principal = NavidromeSessionUser(
                client.id,
                client.username,
                userClient,
                client.name,
                client.lastFMApiKey
            )

            userRepository.createNewUser(principal.id)
            return UsernamePasswordAuthenticationToken(principal, "hidden-password", roles)
        } catch (e: Exception) {
            throw BadCredentialsException("Couldn't log in to navidrome client", e)
        }

        throw BadCredentialsException("Couldn't log in to navidrome client")
    }
}