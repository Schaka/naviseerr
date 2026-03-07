package com.github.schaka.naviseerr.download.processing

import com.github.schaka.naviseerr.download.monitor.ProcessableDownload
import com.github.schaka.naviseerr.tagging.TaggingClient
import com.github.schaka.naviseerr.tagging.TaggingRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
class OneTaggerPostProcessingService(
    private val taggingClient: TaggingClient
) : PostProcessingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun process(download: ProcessableDownload) {
        val directory = download.files
            .map { Path.of(it.filePath).parent }
            .firstOrNull() ?: return

        val response = taggingClient.oneTaggerTag(TaggingRequest(directory.toString()))
        if (response.success) {
            log.info("OneTagger completed for job {} in {}ms", download.id, response.durationMs)
        } else {
            log.warn("OneTagger failed for job {} (exit {}): {}", download.id, response.exitCode, response.stderr)
        }
    }
}
