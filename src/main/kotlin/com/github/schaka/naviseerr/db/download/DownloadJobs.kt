package com.github.schaka.naviseerr.db.download

import com.github.schaka.naviseerr.db.download.enums.DownloadJobStatus
import com.github.schaka.naviseerr.db.download.enums.DownloadJobType
import com.github.schaka.naviseerr.db.download.enums.DownloadProtocol
import com.github.schaka.naviseerr.db.library.MediaRequests
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp

object DownloadJobs : UUIDTable("download_jobs") {
    val mediaRequestId = reference("media_request_id", MediaRequests).nullable()
    val jobType = enumerationByName<DownloadJobType>("job_type", 16)
    val status = enumerationByName<DownloadJobStatus>("status", 32)
    val artistName = varchar("artist_name", 512)
    val albumTitle = varchar("album_title", 512).nullable()
    val musicbrainzArtistId = varchar("musicbrainz_artist_id", 36).nullable()
    val musicbrainzAlbumId = varchar("musicbrainz_album_id", 36).nullable()
    val lidarrArtistId = long("lidarr_artist_id").nullable()
    val lidarrAlbumId = long("lidarr_album_id").nullable()
    val lidarrHistoryId = long("lidarr_history_id").nullable()
    val downloadClient = varchar("download_client", 128).nullable()
    val downloadProtocol = enumerationByName<DownloadProtocol>("download_protocol", 32).nullable()
    val slskdUsername = varchar("slskd_username", 256).nullable()
    val retryCount = integer("retry_count").default(0)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
