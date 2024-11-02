package com.github.schaka.naviseerr.music_library.lidarr.dto.quality

data class QualityDefinition(
    val id: Int,
    val quality: Quality,
    val revision: QualityRevision = QualityRevision()
) {
}