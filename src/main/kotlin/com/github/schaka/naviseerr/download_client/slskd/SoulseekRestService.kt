package com.github.schaka.naviseerr.download_client.slskd

import com.github.schaka.naviseerr.download_client.slskd.dto.*
import com.github.schaka.naviseerr.download_client.slskd.dto.download.DownloadRequest
import com.github.schaka.naviseerr.download_client.slskd.dto.download.TransferFile
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import org.springframework.stereotype.Service

@Service
class SoulseekRestService(

    private val soulseekClient: SoulseekClient,
    private val soulseekProperties: SoulseekProperties
) {

    private val log = KotlinLogging.logger {}

    suspend fun search(searchText: String): List<SearchResult> {
        val search = Search(searchText)
        soulseekClient.searchForMedia(search)
        waitForSearch(search.id)
        val searchResult = soulseekClient.searchContent(search.id)
        soulseekClient.deleteSearch(search.id)
        return searchResult
    }

    // TODO: re-try on error status
    // handle downloads across several users
    suspend fun download(result: SearchMatchResult): List<TransferFile> {
        val filesToDownload = result.tracks
            .filter { it.file != null }
            .map { it.file!! }
            .map { DownloadRequest(it.filename, it.size) }

        if (filesToDownload.isNotEmpty()) {
            log.trace { "Triggered download for $filesToDownload" }
            soulseekClient.startDownload(result.result.username, filesToDownload)
        }
        val downloadResults = waitForDownload(result)
        return downloadResults
    }

    suspend fun waitForSearch(id: String) {
        while (!soulseekClient.searches(id).isComplete) {
            delay(500)
        }
    }

    suspend fun waitForDownload(result: SearchMatchResult): List<TransferFile> {
        val targetFiles = result.tracks.map { it.file?.filename }.toSet()
        delay(500)
        soulseekClient.getDownloads(result.result.username).directories
        while (getDownloads(result.result.username, targetFiles).any { !it.isComplete() }) {
            delay(500)
        }
        log.debug { "Finished download from user: ${result.result.username}" }
        return getDownloads(result.result.username, targetFiles)
    }

    suspend fun getDownloads(username: String, targetFiles: Set<String?>): List<TransferFile> {
        return soulseekClient.getDownloads(username).directories.flatMap { it.files }.filter{ targetFiles.contains(it.filename) }
    }


}