package com.github.schaka.naviseerr.db.download

import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp

object DownloadSourceBlacklist : UUIDTable("download_source_blacklist") {
    val jobId = reference("job_id", DownloadJobs)
    val sourceIdentifier = varchar("source_identifier", 512)
    val blacklistedAt = timestamp("blacklisted_at")
}
