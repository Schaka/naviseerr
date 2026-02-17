package com.github.schaka.naviseerr.lastfm

import com.github.schaka.naviseerr.lastfm.nokey.LastFMPublicApiClient
import com.github.schaka.naviseerr.db.user.NaviseerrUser
import com.github.schaka.naviseerr.db.user.NaviseerrUserService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder

@Service
class LastFMAuthService(
    private val properties: LastFMProperties,
    private val lastFmApiClient: LastFMPublicApiClient,
    private val userService: NaviseerrUserService,
) {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        private const val LASTFM_AUTH_URL = "https://www.last.fm/api/auth/"
    }

    /**
     * Generates the Last.fm authorization URL for a user to connect their account.
     * The callback URL should be configured to return to the frontend settings page.
     */
    fun generateAuthUrl(callbackUrl: String): String {
        return UriComponentsBuilder.fromUriString(LASTFM_AUTH_URL)
            .queryParam("api_key", properties.apiKey)
            .queryParam("cb", callbackUrl)
            .build()
            .toUriString()
    }

    /**
     * Exchanges a Last.fm authorization token for a session key.
     * The session key is valid indefinitely unless revoked by the user.
     *
     * @param token The token received from Last.fm after user authorization
     * @param user The authenticated user to associate the session key with
     * @return The session key if successful
     * @throws IllegalStateException if the user already has a session key
     */
    fun exchangeTokenForSessionKey(token: String, user: NaviseerrUser): String {
        if (user.lastFmSessionKey != null) {
            throw IllegalStateException("User ${user.username} already has a Last.fm session key")
        }

        log.debug("Exchanging token for session key for user: {}", user.username)

        // Generate API signature for auth.getSession call using shared utility
        val signature = LastFMSignatureUtil.generateSignature(
            params = mapOf(
                "method" to "auth.getSession",
                "api_key" to properties.apiKey,
                "token" to token
            ),
            sharedSecret = properties.sharedSecret
        )

        // Call Last.fm API to get session
        val response = lastFmApiClient.getSession(
            apiKey = properties.apiKey,
            token = token,
            apiSig = signature
        )

        val sessionKey = response.session.key
        log.debug("Successfully obtained session key for user: {}", user.username)

        userService.updateLastFMSessionKey(user.username, sessionKey)

        return sessionKey
    }
}