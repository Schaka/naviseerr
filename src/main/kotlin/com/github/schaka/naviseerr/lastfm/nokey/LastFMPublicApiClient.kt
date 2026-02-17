package com.github.schaka.naviseerr.lastfm.nokey

import com.github.schaka.naviseerr.lastfm.dto.LastFMSessionResponse
import feign.Param
import feign.RequestLine

/**
 * Feign client for public (unauthenticated) Last.fm API calls.
 * Used for authentication flow before a session key is obtained.
 */
interface LastFMPublicApiClient {

    /**
     * Exchanges an authentication token for a session key.
     * The token is valid for 60 minutes after being granted.
     *
     * @param apiKey The application's API key
     * @param token The authentication token received from Last.fm callback
     * @param apiSig MD5 signature of the request parameters
     * @return Session response containing the session key
     */
    @RequestLine("GET /?method=auth.getSession&api_key={api_key}&token={token}&api_sig={api_sig}")
    fun getSession(
        @Param("api_key") apiKey: String,
        @Param("token") token: String,
        @Param("api_sig") apiSig: String
    ): LastFMSessionResponse
}