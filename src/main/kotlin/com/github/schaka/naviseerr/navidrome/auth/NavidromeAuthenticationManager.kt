package com.github.schaka.naviseerr.navidrome.auth

import com.github.schaka.naviseerr.navidrome.polling.NavidromeClientFactory
import com.github.schaka.naviseerr.navidrome.polling.SubsonicClientFactory
import com.github.schaka.naviseerr.db.user.NaviseerrUserService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component

@Component
class NavidromeAuthenticationManager(
    private val navidromeLoginClient: NavidromeLoginClient,
    private val navidromeClientFactory: NavidromeClientFactory,
    private val navidromeSubsonicClientFactory: SubsonicClientFactory,
    private val userService: NaviseerrUserService
) : AuthenticationManager {

    override fun authenticate(authentication: Authentication): Authentication {
        val username = authentication.name
        val password = authentication.credentials.toString()

        try {

            val navidromeUser = navidromeLoginClient.authenticate(username, password)

            val user = userService.storeUserLogin(
                username,
                navidromeUser.id,
                navidromeUser.token,
                navidromeUser.subsonicToken,
                navidromeUser.subsonicSalt
            )

            // invalidate caches, because a new login resets the subsonic token/salt and sometimes the navidrome token
            navidromeClientFactory.invalidateClient(user.id)
            navidromeSubsonicClientFactory.invalidateClient(user.id)

            val authorities = mutableListOf(SimpleGrantedAuthority("ROLE_USER"))
            if (navidromeUser.isAdmin) {
                authorities.add(SimpleGrantedAuthority("ROLE_ADMIN"))
            }

            return UsernamePasswordAuthenticationToken(
                user,
                "hidden-passwowrd",
                authorities
            )

        } catch (e: Exception) {
            throw BadCredentialsException("Couldn't log in to Navidrome", e)
        }
    }
}
