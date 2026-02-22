package com.github.schaka.naviseerr.download.processing

import com.github.schaka.naviseerr.db.download.DownloadJob
import com.github.schaka.naviseerr.db.download.DownloadJobFile

interface LibraryImportService {
    fun importToLibrary(job: DownloadJob, files: List<DownloadJobFile>)
}
