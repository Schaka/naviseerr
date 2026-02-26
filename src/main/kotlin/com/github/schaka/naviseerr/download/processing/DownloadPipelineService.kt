package com.github.schaka.naviseerr.download.processing

import com.github.schaka.naviseerr.db.download.enums.DownloadJobStatus
import com.github.schaka.naviseerr.db.download.enums.FileProcessingStatus
import com.github.schaka.naviseerr.download.monitor.DownloadMonitorService
import com.github.schaka.naviseerr.download.monitor.ProcessableDownload
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class DownloadPipelineService(
    private val monitorServices: List<DownloadMonitorService>,
    private val acoustIdService: AcoustIdService,
    private val postProcessingService: PostProcessingService,
    private val libraryImportService: LibraryImportService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    fun process() {
        for (service in monitorServices) {
            for (download in service.findReadyForProcessing()) {
                processJob(service, download)
            }
        }
    }

    private fun processJob(service: DownloadMonitorService, download: ProcessableDownload) {
        when (download.status) {
            DownloadJobStatus.ACOUSTID_PENDING -> {
                processAcoustId(service, download)
                val needsImport = processPostProcessing(service, download)
                if (needsImport) processImport(service, download)
            }
            DownloadJobStatus.POST_PROCESSING -> {
                val needsImport = processPostProcessing(service, download)
                if (needsImport) processImport(service, download)
            }
            DownloadJobStatus.IMPORT_PENDING -> processImport(service, download)
            else -> {}
        }
    }

    private fun processAcoustId(service: DownloadMonitorService, download: ProcessableDownload) {
        for (file in download.files) {
            val result = acoustIdService.recognize(file.filePath)
            val status = if (result == AcoustIdResult.RECOGNIZED) FileProcessingStatus.RECOGNIZED else FileProcessingStatus.UNRECOGNIZED
            service.updateFileAcoustId(file.id, status)
        }
        log.info("AcoustID done for job {}, advancing to POST_PROCESSING", download.id)
        service.updateJobStatus(download.id, DownloadJobStatus.POST_PROCESSING)
    }

    private fun processPostProcessing(service: DownloadMonitorService, download: ProcessableDownload): Boolean {
        postProcessingService.process(download)
        download.files.forEach { service.updateFilePostProcessing(it.id, FileProcessingStatus.COMPLETED) }

        return if (service.needsImport()) {
            log.info("Post-processing done for job {}, advancing to IMPORT_PENDING", download.id)
            service.updateJobStatus(download.id, DownloadJobStatus.IMPORT_PENDING)
            true
        } else {
            log.info("Post-processing done for job {}, marking COMPLETED", download.id)
            service.markCompleted(download)
            service.cleanup(download)
            false
        }
    }

    private fun processImport(service: DownloadMonitorService, download: ProcessableDownload) {
        libraryImportService.importToLibrary(download)
        download.files.forEach { service.updateFileImportStatus(it.id, FileProcessingStatus.COMPLETED) }
        log.info("Import done for job {}, marking COMPLETED", download.id)
        service.markCompleted(download)
        service.cleanup(download)
    }
}
