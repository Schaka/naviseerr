package com.github.schaka.naviseerr.db.download.enums

enum class DownloadJobStatus {
    DOWNLOADING,
    ACOUSTID_PENDING,
    POST_PROCESSING,
    IMPORT_PENDING,
    COMPLETED,
    FAILED
}
