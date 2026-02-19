package com.github.schaka.naviseerr.library.dto

import com.github.schaka.naviseerr.db.library.MediaRequest

data class ArtistRequestDto(
    val musicbrainzId: String,
    val name: String
)

data class AlbumRequestDto(
    val musicbrainzArtistId: String,
    val musicbrainzAlbumId: String,
    val artistName: String,
    val albumTitle: String
)

data class MediaRequestDto(
    val id: String,
    val artistName: String,
    val albumTitle: String?,
    val status: String,
    val musicbrainzArtistId: String,
    val musicbrainzAlbumId: String?,
    val createdAt: String,
    val updatedAt: String
)

fun MediaRequest.toDto() = MediaRequestDto(
    id = id.toString(),
    artistName = artistName,
    albumTitle = albumTitle,
    status = status.name,
    musicbrainzArtistId = musicbrainzArtistId,
    musicbrainzAlbumId = musicbrainzAlbumId,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString()
)
