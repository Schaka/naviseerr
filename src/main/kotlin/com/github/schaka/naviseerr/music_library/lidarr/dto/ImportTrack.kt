package com.github.schaka.naviseerr.music_library.lidarr.dto

import com.github.schaka.naviseerr.music_library.lidarr.dto.quality.QualityDefinition

data class ImportTrack(
    val path: String,
    val artistId: Long,
    val albumId: Long,
    val trackIds: List<Long>,
    val quality: QualityDefinition,
    val albumReleaseId: Long? = null,
    val indexerFlags: Int = 0,
    val disableReleaseSwitching: Boolean = false,
)
