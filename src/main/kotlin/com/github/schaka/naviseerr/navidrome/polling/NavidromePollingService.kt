package com.github.schaka.naviseerr.navidrome.polling

import com.github.schaka.naviseerr.db.user.NaviseerrUser
import com.github.schaka.naviseerr.db.user.NaviseerrUserService
import com.github.schaka.naviseerr.navidrome.auth.NavidromeAdminClientConfig.Companion.SUBSONIC_ADMIN_CLIENT
import com.github.schaka.naviseerr.navidrome.scrobble.NavidromeScrobblingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * Polls Navidrome for user listening activity and stores it for scrobbling.
 * Runs on virtual threads every 10 seconds.
 */
@Service
class NavidromePollingService(
    private val subsonicClientFactory: SubsonicClientFactory,
    @Qualifier(SUBSONIC_ADMIN_CLIENT)
    private val navidromeAdminSubsonicClient: NavidromeSubsonicClient,
    private val userService: NaviseerrUserService,
    private val scrobblingService: NavidromeScrobblingService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Polls Navidrome activity for all users.
     * Scheduled to run every 10 seconds on virtual threads.
     */
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    fun pollActivity() {
        try {
            val users = userService.getUsersWithApiKeys()
            logger.debug("Polling activity for {} users", users.size)

            users.forEach { user ->
                pollUserActivity(user)
            }
        } catch (e: Exception) {
            logger.error("Error during activity polling", e)
        }
    }

    private fun pollUserActivity(user: NaviseerrUser) {
        try {
            val subsonicClient = subsonicClientFactory.getOrCreateClient(user)

            val allNowPlaying = subsonicClient.getNowPlaying()
            val userNowPlaying = allNowPlaying.subsonicResponse.nowPlaying?.entry?.firstOrNull { it.username == user.username }

            userNowPlaying
            ?.let { scrobblingService.processNowPlaying(user, it, subsonicClient) }
                ?: scrobblingService.processPlaybackStopped(user, subsonicClient)


        } catch (e: Exception) {
            logger.error("Error polling activity for user: {}", user.username, e)
        }
    }
}
