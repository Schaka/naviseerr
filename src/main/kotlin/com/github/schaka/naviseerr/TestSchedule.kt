package com.github.schaka.naviseerr

import com.github.schaka.naviseerr.download_client.slskd.SoulseekRestService
import com.github.schaka.naviseerr.music_library.library.LibraryManager
import com.github.schaka.naviseerr.music_library.lidarr.LidarrRestService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TestSchedule(
    val lidarrRestService: LidarrRestService,
    val soulseekRestService: SoulseekRestService,
    val libraryManager: LibraryManager
) {

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    suspend fun schedule() {
        //libraryManager.updateLibrary()
        val result = soulseekRestService.search("Modern Baseball")
        println("test")
    }
}