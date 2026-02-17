package com.github.schaka.naviseerr.navidrome.activity

import com.github.schaka.naviseerr.navidrome.polling.NavidromeClientFactory
import com.github.schaka.naviseerr.navidrome.polling.NavidromeSubsonicClient
import com.github.schaka.naviseerr.navidrome.polling.dto.NavidromeSong
import com.github.schaka.naviseerr.db.activity.UserPlay
import com.github.schaka.naviseerr.db.activity.UserPlayService
import com.github.schaka.naviseerr.navidrome.polling.SubsonicClientFactory
import com.github.schaka.naviseerr.db.user.NaviseerrUser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class NavidromeBackfillService(
    private val navidromeClientFactory: NavidromeClientFactory,
    private val subsonicClientFactory: SubsonicClientFactory,
    private val userPlayService: UserPlayService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Backfill user activity from Navidrome into the local database.
     * Fetches the specified number of most recent plays and stores them if not already present.
     *
     * @param user The user whose activity to backfill
     * @param count Number of recent plays to fetch (default: 100, max: 500)
     * @return Number of new plays that were stored
     */
    fun backfillActivity(
        user: NaviseerrUser,
        count: Int = 100
    ): Int {
        log.info("Starting backfill for user {} with {} plays", user.username, count)

        val client = navidromeClientFactory.getOrCreateClient(user)
        val subsonicClient = subsonicClientFactory.getOrCreateClient(user)
        val activity = client.getActivity(size = count)

        if (activity.isEmpty()) {
            log.info("No activity found for user: {}", user.username)
            return 0
        }

        var newPlaysCount = 0

        activity.forEach { song ->
            val play = createUserPlay(user, song, subsonicClient)
            val savedPlay = userPlayService.createPlay(play)

            if (savedPlay.id == play.id) {
                newPlaysCount++
                log.debug("Stored backfilled play: {} - {} ({})", song.artist, song.title, song.playDate)
            } else {
                log.debug("Skipped duplicate play: {} - {} ({})", song.artist, song.title, song.playDate)
            }
        }

        log.info(
            "Backfill complete for user {}: {} new plays stored, {} duplicates skipped",
            user.username,
            newPlaysCount,
            activity.size - newPlaysCount
        )

        return newPlaysCount
    }

    /**
     * Create a UserPlay entity from a NavidromeSong.
     * Enriches with MusicBrainz IDs if available.
     */
    private fun createUserPlay(
        user: NaviseerrUser,
        song: NavidromeSong,
        subsonicClient: NavidromeSubsonicClient
    ): UserPlay {
        val playDate = song.playDate ?: Instant.now()

        var play = UserPlay(
            id = UUID.randomUUID(),
            userId = user.id,
            navidromePlayId = "${song.id}-${playDate.toEpochMilli()}",
            trackId = song.id,
            trackName = song.title,
            artistName = song.artist,
            albumName = song.album,
            duration = song.duration.toInt(),
            playedAt = playDate,
            musicBrainzTrackId = song.mbzTrackId,
            musicBrainzArtistId = song.mbzArtistId,
            musicBrainzAlbumId = song.mbzAlbumId
        )

        if (play.musicBrainzTrackId == null) {
            play = enrichWithMusicBrainzIds(play, subsonicClient)
        }

        return play
    }

    /**
     * Enrich a UserPlay with MusicBrainz IDs by fetching full song details.
     * This is necessary because not all endpoints return MusicBrainz metadata.
     */
    fun enrichWithMusicBrainzIds(
        play: UserPlay,
        subsonicClient: NavidromeSubsonicClient
    ): UserPlay {
        return try {
            val songResponse = subsonicClient.getSong(play.trackId)
            val song = songResponse.subsonicResponse.song

            if (song != null) {
                return play.copy(musicBrainzTrackId = song.musicBrainzId)
            }

            log.warn("Failed to fetch song details for track: {}", play.trackId)
            return play
        } catch (e: Exception) {
            log.warn("Error fetching MusicBrainz IDs for track {}: {}", play.trackId, e.message)
            play
        }
    }
}