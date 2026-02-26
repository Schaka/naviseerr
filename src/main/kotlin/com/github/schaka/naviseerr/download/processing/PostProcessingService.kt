package com.github.schaka.naviseerr.download.processing

import com.github.schaka.naviseerr.download.monitor.ProcessableDownload

interface PostProcessingService {
    fun process(download: ProcessableDownload)
}
