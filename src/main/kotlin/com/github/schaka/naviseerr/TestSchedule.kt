package com.github.schaka.naviseerr

import com.github.schaka.naviseerr.music_library.library.LibraryManager
import com.github.schaka.naviseerr.music_library.lidarr.LidarrRestService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TestSchedule(
    val lidarrRestService: LidarrRestService,
    val libraryManager: LibraryManager
) {

    @Scheduled(fixedDelay = 1000L)
    fun schedule() {
        libraryManager.updateLibrary()
    }
}