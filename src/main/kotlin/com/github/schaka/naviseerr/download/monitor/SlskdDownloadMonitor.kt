package com.github.schaka.naviseerr.download.monitor

import com.github.schaka.naviseerr.db.download.DownloadJobService
import com.github.schaka.naviseerr.db.download.enums.DownloadJobStatus
import com.github.schaka.naviseerr.db.download.enums.DownloadJobType
import com.github.schaka.naviseerr.slskd.SoulseekClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class SlskdDownloadMonitor(
    private val soulseekClient: SoulseekClient,
    private val downloadJobService: DownloadJobService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    fun checkSlskdDownloads() {
        val activeJobs = downloadJobService.findByTypeAndStatus(DownloadJobType.SLSKD, DownloadJobStatus.DOWNLOADING)

        for (job in activeJobs) {
            val username = job.slskdUsername ?: continue
            val userDownloads = soulseekClient.getDownloads(username)
            val allComplete = userDownloads.directories.all { it.isComplete() }

            if (allComplete) {
                log.info("slskd download complete for job {} (user: {}), advancing to ACOUSTID_PENDING", job.id, username)
                downloadJobService.updateStatus(job.id, DownloadJobStatus.ACOUSTID_PENDING)
            }
        }
    }
}
