package com.github.schaka.naviseerr.library

import com.github.schaka.naviseerr.lidarr.LidarrClient
import com.github.schaka.naviseerr.library.dto.DownloadItemDto
import com.github.schaka.naviseerr.library.dto.DownloadQueueDto
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/downloads")
class DownloadController(private val lidarrClient: LidarrClient) {

    @GetMapping("/queue")
    fun getQueue(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): ResponseEntity<DownloadQueueDto> {
        val queue = lidarrClient.getQueue(PageRequest.of(page, pageSize))
        val items = queue.content.map { item ->
            val progress = if (item.size > 0) ((item.size - item.sizeleft) / item.size) * 100.0 else 0.0
            DownloadItemDto(
                id = item.id,
                artistName = item.artist?.artistName ?: "Unknown",
                albumTitle = item.album?.title ?: item.title,
                progress = progress,
                status = item.status,
                timeleft = item.timeleft,
                estimatedCompletionTime = item.estimatedCompletionTime,
                protocol = item.protocol
            )
        }
        return ResponseEntity.ok(DownloadQueueDto(items, queue.totalElements))
    }
}
