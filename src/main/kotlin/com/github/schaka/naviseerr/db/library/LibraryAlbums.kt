package com.github.schaka.naviseerr.db.library

import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp

object LibraryAlbums : UUIDTable("library_albums") {
    val artistId = reference("artist_id", LibraryArtists)
    val musicbrainzId = varchar("musicbrainz_id", 36).nullable().uniqueIndex()
    val lidarrId = long("lidarr_id").nullable().uniqueIndex()
    val title = varchar("title", 512)
    val albumType = varchar("album_type", 64).nullable()
    val status = varchar("status", 32).default("UNKNOWN")
    val mediaSource = varchar("source", 32).default("LIDARR")
    val syncedAt = timestamp("synced_at")
}
