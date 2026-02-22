package com.github.schaka.naviseerr.download.processing

import com.github.schaka.naviseerr.db.download.DownloadJob
import com.github.schaka.naviseerr.db.download.DownloadJobFile
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DummyLibraryImportService : LibraryImportService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun importToLibrary(job: DownloadJob, files: List<DownloadJobFile>) {
        log.info("Library import would run for job {} ({} files)", job.id, files.size)
    }
}
