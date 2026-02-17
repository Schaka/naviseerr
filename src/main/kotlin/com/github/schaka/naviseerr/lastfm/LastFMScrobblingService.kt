package com.github.schaka.naviseerr.lastfm

import com.github.schaka.naviseerr.db.activity.UserPlay
import com.github.schaka.naviseerr.db.user.NaviseerrUser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service responsible for immediately scrobbling plays to Last.fm.
 * Called synchronously when a new play is detected.
 * Does NOT pull from database - scrobbles are sent immediately.
 */
@Service
class LastFMScrobblingService(
    private val lastFMClientFactory: LastFMClientFactory
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Immediately scrobble a play to Last.fm.
     * Called when a track finishes (detected by new track starting in nowPlaying).
     *
     * @param user The user whose play is being scrobbled
     * @param play The play to scrobble
     * @return true if scrobble was successful, false otherwise
     */
    fun scrobble(user: NaviseerrUser, play: UserPlay): Boolean {

        if (!user.lastFmScrobblingEnabled) {
            log.debug("User {} has Last.fm scrobbling disabled, skipping", user.username)
            return false
        }

        if (user.lastFmSessionKey == null) {
            log.debug("User {} has no Last.fm session key, skipping scrobble", user.username)
            return false
        }

        if (!isValidForScrobbling(play)) {
            log.debug("Play does not meet Last.fm requirements: {} - {}", play.artistName, play.trackName)
            return false
        }

        return try {
            val client = lastFMClientFactory.getOrCreateClient(user)

            client.scrobbleTrack(
                artist = play.artistName,
                track = play.trackName,
                timestamp = play.playedAt.epochSecond,
                album = play.albumName,
                albumArtist = null,
                duration = play.duration
            )

            log.info("Scrobbled to Last.fm for user {}: {} - {} ({})",
                user.username, play.artistName, play.trackName, play.albumName)
            true
        } catch (e: Exception) {
            log.error("Failed to scrobble to Last.fm for user {}: {} - {}",
                user.username, play.artistName, play.trackName, e)
            false
        }
    }

    /**
     * Check if a play meets Last.fm's scrobbling requirements.
     *
     * Requirements:
     * - Track duration must be > 30 seconds
     * - Must have artist and track name
     *
     * Note: Last.fm also requires the track to be played for at least 50% or 4 minutes.
     * We assume Navidrome's nowPlaying detection means the track was sufficiently played.
     */
    private fun isValidForScrobbling(play: UserPlay): Boolean {
        // Must be longer than 30 seconds
        if (play.duration <= 30) {
            log.debug("Track too short for scrobbling: {} seconds", play.duration)
            return false
        }

        // Must have artist and track name
        if (play.artistName.isBlank() || play.trackName.isBlank()) {
            log.debug("Missing artist or track name")
            return false
        }

        return true
    }
}
