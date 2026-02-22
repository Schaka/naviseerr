package com.github.schaka.naviseerr.library.dto

data class SearchResultDto<T>(
    val results: List<T>,
    val totalCount: Int
)

data class ArtistSearchResultDto(
    val musicbrainzId: String,
    val name: String,
    val disambiguation: String?,
    val type: String?,
    val country: String?,
    val status: String?
)

data class AlbumSearchResultDto(
    val musicbrainzId: String,
    val title: String,
    val primaryType: String?,
    val firstReleaseDate: String?,
    val artistName: String,
    val artistMusicbrainzId: String,
    val status: String?
)
