package com.github.schaka.naviseerr

import com.github.schaka.naviseerr.download_client.slskd.SoulseekRestService
import com.github.schaka.naviseerr.music_library.library.DownloadManager
import com.github.schaka.naviseerr.music_library.library.LibraryManager
import com.github.schaka.naviseerr.music_library.lidarr.LidarrRestService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TestSchedule(
    val lidarrRestService: LidarrRestService,
    val soulseekRestService: SoulseekRestService,
    val downloadManager: DownloadManager,
    val libraryManager: LibraryManager
) {

    private val log = KotlinLogging.logger {  }

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    suspend fun schedule() {
        //libraryManager.updateLibrary()
        // TODO: store which ones we've already tried and don't download them again until x days later
        val toDownload = lidarrRestService.getMissing(PageRequest.of(0, 5))
        for(missing in toDownload) {
            downloadManager.downloadRelease(missing)
        }
    }
}