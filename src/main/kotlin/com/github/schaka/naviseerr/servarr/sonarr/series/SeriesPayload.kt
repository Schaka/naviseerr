package com.github.schaka.naviseerr.servarr.sonarr.series

data class SeriesPayload(
        val cleanTitle: String,
        val id: Int,
        val imdbId: String?,
        val monitored: Boolean,
        val path: String,
        val qualityProfileId: Int,
        val rootFolderPath: String,
        val seasonFolder: Boolean,
        val seasons: List<Season>,
        val seriesType: String,
        val tags: List<Int>,
        val title: String,
        val titleSlug: String,
        val tvMazeId: Int,
        val tvRageId: Int,
        val tvdbId: Int,
        val year: Int
)