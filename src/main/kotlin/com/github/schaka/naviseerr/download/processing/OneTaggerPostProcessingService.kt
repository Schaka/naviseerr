package com.github.schaka.naviseerr.download.processing

import com.github.schaka.naviseerr.download.monitor.ProcessableDownload
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OneTaggerPostProcessingService : PostProcessingService {

    private val log = LoggerFactory.getLogger(javaClass)

    private val binaryPath = System.getenv("ONE_TAGGER_PATH") ?: System.getProperty("ONE_TAGGER_PATH") ?: IllegalStateException("ONE_TAGGER_PATH environment variable is missing")

    override fun process(download: ProcessableDownload) {
        log.info("Post-processing would run for job {} ({} files)", download.id, download.files.size)
    }
}
