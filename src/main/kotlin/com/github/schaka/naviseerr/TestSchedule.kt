package com.github.schaka.naviseerr

import com.github.schaka.naviseerr.music_library.lidarr.LidarrRestService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TestSchedule(
    val lidarrRestService: LidarrRestService
) {

    @Scheduled(fixedDelay = 1000L)
    fun schedule() {
        lidarrRestService.getLibrary()
    }
}