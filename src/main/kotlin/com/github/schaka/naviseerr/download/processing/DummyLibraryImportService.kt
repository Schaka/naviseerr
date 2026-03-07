package com.github.schaka.naviseerr.download.processing

import com.github.schaka.naviseerr.download.monitor.ProcessableDownload
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DummyLibraryImportService : LibraryImportService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun importToLibrary(download: ProcessableDownload) {
        log.info("Library import would run for job {} ({} files)", download.id, download.files.size)
    }
}
