package com.github.schaka.naviseerr.navidrome.polling

import com.github.schaka.naviseerr.navidrome.polling.dto.SubsonicNowPlayingResponse
import com.github.schaka.naviseerr.navidrome.polling.dto.SubsonicSongResponse
import com.github.schaka.naviseerr.navidrome.polling.dto.SubsonicUserResponse
import feign.Param
import feign.RequestLine

/**
 * Feign client interface for Navidrome's Subsonic API endpoints.
 * Authentication parameters (u, t, s, v, c, f) are provided by the base client configuration.
 */
interface NavidromeSubsonicClient {

    /**
     * Get currently playing tracks for all users.
     * Endpoint: GET /getNowPlaying.view
     */
    @RequestLine("GET /getNowPlaying.view")
    fun getNowPlaying(): SubsonicNowPlayingResponse

    /**
     * Get detailed information about a specific song/track.
     * Endpoint: GET /getSong.view
     */
    @RequestLine("GET /getSong.view?id={id}")
    fun getSong(
        @Param("id") id: String
    ): SubsonicSongResponse

    /**
     * Get user information including scrobbling settings.
     * Endpoint: GET /getUser.view
     */
    @RequestLine("GET /getUser.view?username={username}")
    fun getUser(
        @Param("username") targetUsername: String
    ): SubsonicUserResponse
}
