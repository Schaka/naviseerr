package com.github.schaka.naviseerr.db.activity

import com.github.schaka.naviseerr.db.user.NaviseerrUsers
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp

object UserPlays : UUIDTable("user_plays") {
    val userId = reference("user_id", NaviseerrUsers)
    val navidromePlayId = varchar("navidrome_play_id", 255).uniqueIndex()
    val trackId = varchar("track_id", 255)
    val trackName = varchar("track_name", 512)
    val artistName = varchar("artist_name", 512)
    val albumName = varchar("album_name", 512).nullable()
    val duration = integer("duration")
    val playedAt = timestamp("played_at")
    val musicBrainzTrackId = varchar("musicbrainz_track_id", 36).nullable()
    val musicBrainzArtistId = varchar("musicbrainz_artist_id", 36).nullable()
    val musicBrainzAlbumId = varchar("musicbrainz_album_id", 36).nullable()
    val createdAt = timestamp("created_at")
}
