package com.github.schaka.naviseerr.lastfm

import feign.Param
import feign.RequestLine

/**
 * Feign client for authenticated Last.fm API calls.
 * API documentation: https://www.last.fm/api
 *
 * Authentication (api_key, sk, api_sig) is handled automatically by LastFMRequestInterceptor.
 * Developers only need to define the method and its parameters - no need to worry about auth!
 *
 * Example:
 * ```
 * @RequestLine("GET /?method=track.scrobble&artist={artist}&track={track}&timestamp={timestamp}")
 * fun scrobbleTrack(@Param("artist") artist: String, @Param("track") track: String, @Param("timestamp") timestamp: Long)
 * ```
 */
interface LastFMApiClient {

    /**
     * Scrobble a track to Last.fm.
     * https://www.last.fm/api/show/track.scrobble
     *
     * @param artist The artist name
     * @param track The track name
     * @param timestamp The time the track started playing (Unix timestamp)
     * @param album Optional album name
     * @param albumArtist Optional album artist name
     * @param duration Optional track duration in seconds
     */
    @RequestLine("POST /?method=track.scrobble&artist={artist}&track={track}&timestamp={timestamp}&album={album}&albumArtist={albumArtist}&duration={duration}")
    fun scrobbleTrack(
        @Param("artist") artist: String,
        @Param("track") track: String,
        @Param("timestamp") timestamp: Long,
        @Param("album") album: String? = null,
        @Param("albumArtist") albumArtist: String? = null,
        @Param("duration") duration: Int? = null
    )

    /**
     * Update the current "now playing" track.
     * https://www.last.fm/api/show/track.updateNowPlaying
     *
     * @param artist The artist name
     * @param track The track name
     * @param album Optional album name
     * @param duration Optional track duration in seconds
     */
    @RequestLine("POST /?method=track.updateNowPlaying&artist={artist}&track={track}&album={album}&duration={duration}")
    fun updateNowPlaying(
        @Param("artist") artist: String,
        @Param("track") track: String,
        @Param("album") album: String? = null,
        @Param("duration") duration: Int? = null
    )

    // Additional methods can be added here as needed for Last.fm integration
    // The interceptor automatically handles all authentication
}