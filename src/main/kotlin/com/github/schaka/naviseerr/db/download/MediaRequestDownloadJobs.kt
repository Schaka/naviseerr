package com.github.schaka.naviseerr.db.download

import com.github.schaka.naviseerr.db.library.MediaRequests
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp

object MediaRequestDownloadJobs : UUIDTable("media_request_download_jobs") {
    val mediaRequestId = reference("media_request_id", MediaRequests)
    val jobType = varchar("job_type", 32)
    val createdAt = timestamp("created_at")
}
