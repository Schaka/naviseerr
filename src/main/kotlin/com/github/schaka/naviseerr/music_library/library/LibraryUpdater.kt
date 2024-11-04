package com.github.schaka.naviseerr.music_library.library

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class LibraryUpdater(
    val libraryManager: LibraryManager
) {

    private val log = KotlinLogging.logger {  }

    // 3 hours
    @Scheduled(fixedDelay = 1000 * 60 * 60 * 3)
    fun schedule() {
        libraryManager.updateLibrary()
    }
}