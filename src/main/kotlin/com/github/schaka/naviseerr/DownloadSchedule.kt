package com.github.schaka.naviseerr

import com.github.schaka.naviseerr.download_client.slskd.SoulseekRestService
import com.github.schaka.naviseerr.music_library.library.DownloadManager
import com.github.schaka.naviseerr.music_library.library.LibraryManager
import com.github.schaka.naviseerr.music_library.lidarr.LidarrRestService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Profile("!cds")
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class DownloadSchedule(
    val lidarrRestService: LidarrRestService,
    val soulseekRestService: SoulseekRestService,
    val libraryManager: LibraryManager,
    val downloadManager: DownloadManager
) {

    private val log = KotlinLogging.logger {  }
    private val batchSize = 20

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    suspend fun schedule() {

        val attemptedDownload = ArrayList<Long>(batchSize)
        val validIds = libraryManager.getLidarrIdsForDownloadRun()

        val toDownload = lidarrRestService.getMissing(PageRequest.of(0, batchSize))
        while (toDownload.hasNext()) {
            for (missing in toDownload.content) {
                if (validIds.contains(missing.id)) {
                    log.debug { "Already searched ${missing.title}, trying again later" }
                    continue
                }
                log.info { "Searching Slkd for ${missing.title}" }
                downloadManager.downloadRelease(missing)
                attemptedDownload.add(missing.id)
            }

            // every hour, we try and run this, rather than running it indefinitely
            if (validIds.size >= batchSize) {
                libraryManager.updateLastDownload(validIds)
                break
            }
        }
    }
}