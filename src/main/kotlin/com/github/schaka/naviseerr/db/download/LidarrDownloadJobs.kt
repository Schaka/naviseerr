package com.github.schaka.naviseerr.db.download

import com.github.schaka.naviseerr.db.download.enums.DownloadJobStatus
import com.github.schaka.naviseerr.db.download.enums.DownloadProtocol
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestamp
import java.util.UUID

object LidarrDownloadJobs : IdTable<UUID>("lidarr_download_jobs", ) {

    override val id: Column<EntityID<UUID>> = reference("job_id", foreign = MediaRequestDownloadJobs)
    override val primaryKey = PrimaryKey(id)

    val status = enumerationByName<DownloadJobStatus>("status", 32)
    val artistName = varchar("artist_name", 512)
    val albumTitle = varchar("album_title", 512).nullable()
    val musicbrainzArtistId = varchar("musicbrainz_artist_id", 36).nullable()
    val musicbrainzAlbumId = varchar("musicbrainz_album_id", 36).nullable()
    val lidarrArtistId = long("lidarr_artist_id")
    val lidarrAlbumId = long("lidarr_album_id")
    val lidarrHistoryId = long("lidarr_history_id")
    val downloadClient = varchar("download_client", 128).nullable()
    val downloadProtocol = enumerationByName<DownloadProtocol>("download_protocol", 32)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
