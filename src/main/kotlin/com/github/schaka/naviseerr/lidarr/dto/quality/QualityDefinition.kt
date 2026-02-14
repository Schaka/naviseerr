package com.github.schaka.naviseerr.lidarr.dto.quality

data class QualityDefinition(
    val id: Int,
    val quality: Quality,
    val revision: QualityRevision = QualityRevision()
) {
}