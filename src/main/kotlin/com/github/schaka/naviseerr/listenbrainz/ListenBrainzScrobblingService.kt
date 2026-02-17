package com.github.schaka.naviseerr.listenbrainz

import com.github.schaka.naviseerr.db.activity.UserPlay
import com.github.schaka.naviseerr.listenbrainz.dto.AdditionalInfo
import com.github.schaka.naviseerr.listenbrainz.dto.Listen
import com.github.schaka.naviseerr.listenbrainz.dto.SubmitListensRequest
import com.github.schaka.naviseerr.listenbrainz.dto.TrackMetadata
import com.github.schaka.naviseerr.db.user.NaviseerrUser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service responsible for immediately scrobbling plays to ListenBrainz.
 * Called synchronously when a new play is detected.
 * Does NOT pull from database - scrobbles are sent immediately.
 */
@Service
class ListenBrainzScrobblingService(
    private val listenBrainzClientFactory: ListenBrainzClientFactory
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Immediately scrobble a play to ListenBrainz.
     * Called when a track finishes (detected by new track starting in nowPlaying).
     *
     * @param user The user whose play is being scrobbled
     * @param play The play to scrobble
     * @return true if scrobble was successful, false otherwise
     */
    fun scrobble(user: NaviseerrUser, play: UserPlay): Boolean {

        if (!user.listenBrainzScrobblingEnabled) {
            log.debug("User {} has ListenBrainz scrobbling disabled, skipping", user.username)
            return false
        }

        if (user.listenBrainzToken == null) {
            log.debug("User {} has no ListenBrainz token, skipping scrobble", user.username)
            return false
        }

        if (!isValidForScrobbling(play)) {
            log.debug("Play does not meet ListenBrainz requirements: {} - {}", play.artistName, play.trackName)
            return false
        }

        return try {
            val client = listenBrainzClientFactory.getOrCreateClient(user)

            val listen = Listen(
                listenedAt = play.playedAt.epochSecond,
                trackMetadata = TrackMetadata(
                    artistName = play.artistName,
                    trackName = play.trackName,
                    releaseName = play.albumName,
                    additionalInfo = AdditionalInfo(
                        durationMs = play.duration * 1000
                    )
                )
            )

            val request = SubmitListensRequest(
                listenType = "single",
                payload = listOf(listen)
            )

            client.submitListens(request)

            log.info("Scrobbled to ListenBrainz for user {}: {} - {} ({})",
                user.username, play.artistName, play.trackName, play.albumName)
            true
        } catch (e: Exception) {
            log.error("Failed to scrobble to ListenBrainz for user {}: {} - {}",
                user.username, play.artistName, play.trackName, e)
            false
        }
    }

    /**
     * Check if a play meets ListenBrainz's scrobbling requirements.
     *
     * Requirements:
     * - Must have artist and track name
     *
     * Note: ListenBrainz does not have a minimum duration requirement like Last.fm
     */
    private fun isValidForScrobbling(play: UserPlay): Boolean {
        if (play.artistName.isBlank() || play.trackName.isBlank()) {
            log.debug("Missing artist or track name")
            return false
        }

        return true
    }
}
