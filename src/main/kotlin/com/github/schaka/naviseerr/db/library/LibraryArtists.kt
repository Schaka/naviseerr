package com.github.schaka.naviseerr.db.library

import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp

object LibraryArtists : UUIDTable("library_artists") {
    val musicbrainzId = varchar("musicbrainz_id", 36).nullable().uniqueIndex()
    val lidarrId = long("lidarr_id").nullable().uniqueIndex()
    val name = varchar("name", 512)
    val cleanName = varchar("clean_name", 512).nullable()
    val status = varchar("status", 32).default("UNKNOWN")
    val mediaSource = varchar("source", 32).default("LIDARR")
    val syncedAt = timestamp("synced_at")
}
