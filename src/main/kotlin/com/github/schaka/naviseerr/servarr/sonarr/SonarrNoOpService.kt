package com.github.schaka.naviseerr.servarr.sonarr

import com.github.schaka.naviseerr.servarr.LibraryItem
import com.github.schaka.naviseerr.servarr.ServarrService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SonarrNoOpService : ServarrService {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    override fun getEntries(): List<LibraryItem> {
        log.info("Sonarr is disabled, not getting any shows")
        return listOf()
    }

    override fun removeEntries(items: List<LibraryItem>) {
        log.info("Sonarr is disabled, not deleting any shows")
    }
}