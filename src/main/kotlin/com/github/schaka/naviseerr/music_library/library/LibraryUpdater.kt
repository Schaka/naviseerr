package com.github.schaka.naviseerr.music_library.library

import com.github.schaka.naviseerr.music_library.lidarr.ImportManager
import com.github.schaka.naviseerr.music_library.lidarr.LidarrClient
import com.github.schaka.naviseerr.music_library.lidarr.dto.quality.QualityDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Profile("!cds")
@Component
class LibraryUpdater(
    private val libraryManager: LibraryManager,
    private val importManager: ImportManager,
    private val lidarrClient: LidarrClient,
) {

    private val log = KotlinLogging.logger {  }

    // 3 hours
    @Scheduled(fixedDelay = 1000 * 60 * 60 * 3)
    fun schedule() {
        importManager.qualities = lidarrClient.getQualityDefinitions()
        log.info { "Updated qualities: ${importManager.qualities}" }

        libraryManager.updateLibrary()
    }
}