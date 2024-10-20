package com.github.schaka.naviseerr.servarr.radarr

import com.github.schaka.naviseerr.servarr.LibraryItem
import com.github.schaka.naviseerr.servarr.ServarrService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RadarrNoOpService : ServarrService {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    override fun getEntries(): List<LibraryItem> {
        log.info("Radarr is disabled, not getting any movies")
        return listOf()
    }

    override fun removeEntries(items: List<LibraryItem>) {
        log.info("Radarr is disabled, not deleting any movies")
    }

}