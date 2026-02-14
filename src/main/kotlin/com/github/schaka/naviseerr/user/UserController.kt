package com.github.schaka.naviseerr.user

import com.github.schaka.naviseerr.db.user.NaviseerrUser
import com.github.schaka.naviseerr.db.user.NaviseerrUserService
import com.github.schaka.naviseerr.lastfm.LastFMAuthService
import com.github.schaka.naviseerr.navidrome.activity.NavidromeBackfillService
import com.github.schaka.naviseerr.user.dto.BackfillResultDto
import com.github.schaka.naviseerr.user.dto.settings.LastFMAuthResultDto
import com.github.schaka.naviseerr.user.dto.settings.UpdateUserSettingsRequest
import com.github.schaka.naviseerr.user.dto.settings.UserSettingsDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.RedirectView

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: NaviseerrUserService,
    private val lastFmAuthService: LastFMAuthService,
    private val navidromeBackfillService: NavidromeBackfillService,
) {

    @GetMapping("/settings")
    fun getUserSettings(@AuthenticationPrincipal principal: NaviseerrUser): ResponseEntity<UserSettingsDto> {
        val user = userService.getUserByUsername(principal.username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity.ok(
            UserSettingsDto(
                username = user.username,
                lastFmSessionKey = user.lastFmSessionKey,
                listenBrainzToken = user.listenBrainzToken,
                lastFmScrobblingEnabled = user.lastFmScrobblingEnabled,
                listenBrainzScrobblingEnabled = user.listenBrainzScrobblingEnabled
            )
        )
    }

    @PutMapping("/settings")
    fun updateUserSettings(
        @AuthenticationPrincipal principal: NaviseerrUser,
        @RequestBody request: UpdateUserSettingsRequest
    ): ResponseEntity<UserSettingsDto> {

        val user = userService.getUserByUsername(principal.username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        // Update ListenBrainz token if provided
        if (request.listenBrainzToken != null) {
            userService.updateListenBrainzToken(principal.username, request.listenBrainzToken)
        }

        // Update scrobbling preferences if provided
        if (request.lastFmScrobblingEnabled != null || request.listenBrainzScrobblingEnabled != null) {
            userService.updateScrobblingPreferences(
                username = principal.username,
                lastFmEnabled = request.lastFmScrobblingEnabled,
                listenBrainzEnabled = request.listenBrainzScrobblingEnabled
            )
        }

        // Fetch updated user data
        val updatedUser = userService.getUserByUsername(principal.username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity.ok(
            UserSettingsDto(
                username = updatedUser.username,
                lastFmSessionKey = updatedUser.lastFmSessionKey,
                listenBrainzToken = updatedUser.listenBrainzToken,
                lastFmScrobblingEnabled = updatedUser.lastFmScrobblingEnabled,
                listenBrainzScrobblingEnabled = updatedUser.listenBrainzScrobblingEnabled
            )
        )
    }

    /**
     * Initiates Last.fm OAuth flow by redirecting user to Last.fm authorization page.
     * After authorization, Last.fm will redirect back to the callback endpoint.
     */
    @GetMapping("/lastfm/auth/init")
    fun initializeLastFmAuth(
        @AuthenticationPrincipal principal: NaviseerrUser,
        @RequestParam("callback_url") callbackUrl: String
    ): RedirectView {
        // Check if user already has a session key
        val user = userService.getUserByUsername(principal.username)
        if (user?.lastFmSessionKey != null) {
            // Already connected - redirect back to settings
            return RedirectView(callbackUrl)
        }

        val authUrl = lastFmAuthService.generateAuthUrl(callbackUrl)
        return RedirectView(authUrl)
    }

    /**
     * Handles the callback from Last.fm after user authorization.
     * Exchanges the token for a session key and stores it in the database.
     */
    @GetMapping("/lastfm/auth/callback")
    fun handleLastFmCallback(
        @AuthenticationPrincipal principal: NaviseerrUser,
        @RequestParam("token") token: String
    ): ResponseEntity<LastFMAuthResultDto> {
        return try {
            val sessionKey = lastFmAuthService.exchangeTokenForSessionKey(token, principal)
            ResponseEntity.ok(
                LastFMAuthResultDto(
                    success = true,
                    message = "Successfully connected to Last.fm",
                    sessionKey = sessionKey
                )
            )
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                LastFMAuthResultDto(
                    success = false,
                    message = e.message ?: "User already has a Last.fm connection",
                    sessionKey = null
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                LastFMAuthResultDto(
                    success = false,
                    message = "Failed to connect to Last.fm: ${e.message}",
                    sessionKey = null
                )
            )
        }
    }

    /**
     * Backfill listening history from Navidrome.
     * Fetches recent plays from Navidrome and stores them in the local database.
     * Automatically detects and skips duplicates.
     *
     * @param principal The authenticated user
     * @param count Number of recent plays to fetch (default: 100, max: 500)
     * @return Result of the backfill operation
     */
    @PostMapping("/backfill")
    fun backfillActivity(
        @AuthenticationPrincipal principal: NaviseerrUser,
        @RequestParam(name = "count", defaultValue = "100") count: Int
    ): ResponseEntity<BackfillResultDto> {
        val newPlaysCount = navidromeBackfillService.backfillActivity(principal, count)

        return ResponseEntity.ok(
            BackfillResultDto(
                success = true,
                message = "Successfully backfilled $newPlaysCount new plays",
                newPlaysCount = newPlaysCount,
                requestedCount = count
            )
        )
    }
}
