package com.github.schaka.naviseerr.db.user

import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp

object NaviseerrUsers : UUIDTable("naviseerr_users") {

    val username = varchar("username", 255).uniqueIndex()
    val navidromeId = varchar("navidrome_id", 255)
    val navidromeToken = varchar("navidrome_token", 255)
    val subsonicToken = varchar("subsonic_token", 255)
    val subsonicSalt = varchar("subsonic_salt", 255)
    val lastLogin = timestamp("last_login")
    val lastFMSessionKey = varchar("last_fm_session_key", 255).nullable()
    val listenBrainzApiKey = varchar("listenbrainz_api_key", 255).nullable()
    val lastFmScrobblingEnabled = bool("last_fm_scrobbling_enabled").default(true)
    val listenBrainzScrobblingEnabled = bool("listenbrainz_scrobbling_enabled").default(true)

}