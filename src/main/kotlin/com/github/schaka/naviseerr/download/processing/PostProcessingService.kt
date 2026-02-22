package com.github.schaka.naviseerr.download.processing

import com.github.schaka.naviseerr.db.download.DownloadJob
import com.github.schaka.naviseerr.db.download.DownloadJobFile

interface PostProcessingService {
    fun process(job: DownloadJob, files: List<DownloadJobFile>)
}
