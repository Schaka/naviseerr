package com.github.schaka.naviseerr.download_client.slskd.lucene

import com.github.schaka.naviseerr.download_client.slskd.dto.SearchResult

data class SearchMatchResult(
    val result: SearchResult,
    val score: Double,
    val tracks: List<TrackMatchResult>
)
