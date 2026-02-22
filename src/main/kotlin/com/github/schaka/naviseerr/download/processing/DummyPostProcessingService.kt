package com.github.schaka.naviseerr.download.processing

import com.github.schaka.naviseerr.db.download.DownloadJob
import com.github.schaka.naviseerr.db.download.DownloadJobFile
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DummyPostProcessingService : PostProcessingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun process(job: DownloadJob, files: List<DownloadJobFile>) {
        log.info("Post-processing would run for job {} ({} files)", job.id, files.size)
    }
}
