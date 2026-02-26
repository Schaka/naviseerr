package com.github.schaka.naviseerr.db.library

import com.github.schaka.naviseerr.db.library.enums.RequestStatus
import com.github.schaka.naviseerr.db.user.NaviseerrUsers
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp
import java.util.UUID

object MediaRequests : UUIDTable("media_requests") {
    val userId = reference("user_id", NaviseerrUsers)
    val musicbrainzArtistId = varchar("musicbrainz_artist_id", 36)
    val musicbrainzAlbumId = varchar("musicbrainz_album_id", 36).nullable()
    val artistName = varchar("artist_name", 512)
    val albumTitle = varchar("album_title", 512).nullable()
    val status = enumerationByName<RequestStatus>("status", 32).default(RequestStatus.REQUESTED)
    val lidarrArtistId = long("lidarr_artist_id").nullable()
    val lidarrAlbumId = long("lidarr_album_id").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
