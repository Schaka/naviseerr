package com.github.schaka.naviseerr.download.processing

interface AcoustIdService {
    fun recognize(filePath: String): AcoustIdResult
}

enum class AcoustIdResult {
    RECOGNIZED,
    UNRECOGNIZED
}
