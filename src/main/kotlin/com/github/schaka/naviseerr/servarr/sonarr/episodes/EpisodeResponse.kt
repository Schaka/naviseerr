package com.github.schaka.naviseerr.servarr.sonarr.episodes

data class EpisodeResponse(
        val id: Int,
        val seriesId: Int,
        val tvdbId: Int?,
        val episodeFileId: Int?,
        val seasonNumber: Int,
        val episodeNumber: Int,
        val folder: String?,
        val episodeFile: EpisodeFile?,
        val path: String?,
        val hasFile: Boolean
)
