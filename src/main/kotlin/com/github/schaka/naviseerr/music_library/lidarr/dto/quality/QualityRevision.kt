package com.github.schaka.naviseerr.music_library.lidarr.dto.quality

data class QualityRevision(
    val version: Int = 1,
    val real: Int = 0,
    val isRepack: Boolean = false
)
