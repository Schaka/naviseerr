package com.github.schaka.naviseerr.download.processing

import com.github.schaka.naviseerr.download.monitor.ProcessableDownload
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DummyPostProcessingService : PostProcessingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun process(download: ProcessableDownload) {
        log.info("Post-processing would run for job {} ({} files)", download.id, download.files.size)
    }
}
