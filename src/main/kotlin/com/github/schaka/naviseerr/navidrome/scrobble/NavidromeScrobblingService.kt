package com.github.schaka.naviseerr.navidrome.scrobble

import com.github.schaka.naviseerr.lastfm.LastFMScrobblingService
import com.github.schaka.naviseerr.listenbrainz.ListenBrainzScrobblingService
import com.github.schaka.naviseerr.navidrome.polling.NavidromeSubsonicClient
import com.github.schaka.naviseerr.navidrome.polling.dto.SubsonicNowPlayingEntry
import com.github.schaka.naviseerr.db.activity.UserPlay
import com.github.schaka.naviseerr.db.activity.UserPlayService
import com.github.schaka.naviseerr.db.user.NaviseerrUser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Service responsible for tracking user listening activity, storing plays, and triggering scrobbles.
 * Keeps track of the last play per user to detect when a track finishes.
 *
 * When a new track is detected in nowPlaying, the previous track is considered finished:
 * 1. Store the finished play in database
 * 2. Immediately scrobble to Last.fm (if user has it enabled)
 * 3. Immediately scrobble to ListenBrainz (if user has it enabled)
 * 4. Update tracking state for next detection
 *
 * When playback stops (no nowPlaying), the last tracked play is scrobbled.
 *
 * TODO: Validate that track was played long enough before scrobbling (based on playedAt timestamp)
 * TODO: Handle timezone differences between Navidrome and Naviseerr for accurate play duration calculation
 */
@Service
class NavidromeScrobblingService(
    private val userPlayService: UserPlayService,
    private val lastFMScrobblingService: LastFMScrobblingService,
    private val listenBrainzScrobblingService: ListenBrainzScrobblingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val lastPlayByUser = ConcurrentHashMap<UUID, LastPlay>()

    /**
     * Process nowPlaying from Subsonic API.
     * When we detect a new track, the previous track is considered finished and we:
     * 1. Store it in database
     * 2. Immediately scrobble to Last.fm
     * 3. Immediately scrobble to ListenBrainz
     */
    fun processNowPlaying(
        user: NaviseerrUser,
        nowPlaying: SubsonicNowPlayingEntry,
        subsonicClient: NavidromeSubsonicClient
    ) {
        val currentTrackId = nowPlaying.id
        val lastPlay = lastPlayByUser[user.id]

        if (lastPlay != null && lastPlay.trackId != currentTrackId) {
            storeAndScrobbleFinishedPlay(user, lastPlay, subsonicClient)
        }

        updateLastPlayFromNowPlaying(user.id, nowPlaying.id, nowPlaying.title, Instant.now())
    }

    /**
     * Process when playback has stopped (no nowPlaying entry).
     * Scrobbles the last tracked play if it exists, then clears tracking.
     *
     * @param user The user whose playback stopped
     * @param subsonicClient Client to fetch track details
     */
    fun processPlaybackStopped(
        user: NaviseerrUser,
        subsonicClient: NavidromeSubsonicClient
    ) {
        val lastPlay = lastPlayByUser[user.id]

        if (lastPlay != null) {
            storeAndScrobbleFinishedPlay(user, lastPlay, subsonicClient)
            lastPlayByUser.remove(user.id)
            log.debug("Cleared last play tracking for user {} after playback stopped", user.username)
        }
    }

    /**
     * Store a finished play in database and immediately scrobble to Last.fm and ListenBrainz.
     */
    private fun storeAndScrobbleFinishedPlay(
        user: NaviseerrUser,
        finishedPlay: LastPlay,
        subsonicClient: NavidromeSubsonicClient
    ) {
        try {
            var play = UserPlay(
                id = UUID.randomUUID(),
                userId = user.id,
                navidromePlayId = "${finishedPlay.trackId}-${finishedPlay.playedAt.toEpochMilli()}",
                trackId = finishedPlay.trackId,
                trackName = finishedPlay.trackName,
                artistName = "",
                albumName = null,
                duration = 0,
                playedAt = finishedPlay.playedAt,
                musicBrainzTrackId = null,
                musicBrainzArtistId = null,
                musicBrainzAlbumId = null
            )

            play = enrichPlayFromSubsonic(play, subsonicClient)

            val savedPlay = userPlayService.createPlay(play)

            log.info(
                "Stored finished play for user {}: {} - {} ({})",
                user.username,
                savedPlay.artistName,
                savedPlay.trackName,
                savedPlay.albumName
            )

            lastFMScrobblingService.scrobble(user, savedPlay)
            listenBrainzScrobblingService.scrobble(user, savedPlay)

        } catch (e: Exception) {
            log.error("Failed to store and scrobble finished play for user {}: {}",
                user.username, finishedPlay.trackId, e)
        }
    }

    /**
     * Enrich a UserPlay with full details from Subsonic getSong endpoint.
     */
    private fun enrichPlayFromSubsonic(
        play: UserPlay,
        subsonicClient: NavidromeSubsonicClient
    ): UserPlay {
        return try {
            val songResponse = subsonicClient.getSong(play.trackId)
            val song = songResponse.subsonicResponse.song

            if (song != null) {
                return play.copy(
                    artistName = song.artist,
                    albumName = song.album,
                    duration = song.duration,
                    musicBrainzTrackId = song.musicBrainzId
                )
            }

            return play
        } catch (e: Exception) {
            log.warn("Error fetching song details for track {}: {}", play.trackId, e.message)
            play
        }
    }

    /**
     * Update the last play tracker with current nowPlaying data.
     */
    private fun updateLastPlayFromNowPlaying(
        userId: UUID,
        trackId: String,
        trackName: String,
        playedAt: Instant
    ) {
        lastPlayByUser[userId] = LastPlay(
            trackId = trackId,
            trackName = trackName,
            playedAt = playedAt
        )
    }

    /**
     * Internal data class to track the currently playing song per user.
     */
    private data class LastPlay(
        val trackId: String,
        val trackName: String,
        val playedAt: Instant
    )
}