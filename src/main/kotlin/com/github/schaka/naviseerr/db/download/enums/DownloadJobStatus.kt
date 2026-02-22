package com.github.schaka.naviseerr.db.download.enums

enum class DownloadJobStatus {
    DOWNLOADING,
    ACOUSTID_PENDING,
    ACOUSTID_FAILED,
    POST_PROCESSING,
    IMPORT_PENDING,
    COMPLETED,
    FAILED
}
