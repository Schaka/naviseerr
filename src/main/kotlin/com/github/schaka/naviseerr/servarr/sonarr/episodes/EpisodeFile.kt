package com.github.schaka.naviseerr.servarr.sonarr.episodes

data class EpisodeFile(
        val id: Int,
        val seriesId: Int,
        val seasonNumber: Int,
        val relativePath: String,
        val path: String
)
