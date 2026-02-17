package com.github.schaka.naviseerr.slskd.dto

data class SearchMatchResult(
    val result: SearchResult,
    val score: Double,
    val tracks: List<TrackMatchResult>
)
