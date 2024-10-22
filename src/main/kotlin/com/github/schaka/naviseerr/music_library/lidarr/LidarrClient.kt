package com.github.schaka.naviseerr.music_library.lidarr

import com.github.schaka.naviseerr.music_library.lidarr.dto.Album
import com.github.schaka.naviseerr.music_library.lidarr.dto.Artist
import com.github.schaka.naviseerr.music_library.lidarr.dto.TrackFile
import feign.Param
import feign.RequestLine

interface LidarrClient {

    @RequestLine("GET /artist")
    fun getAllArtists(): List<Artist>

    @RequestLine("GET /album?artistId={artistId}&includeAllArtistAlbums=true")
    fun getAlbums(@Param("artistId") artistId: Long): List<Album>

    @RequestLine("GET /album?includeAllArtistAlbums=true")
    fun getAllAlbums(): List<Album>

    @RequestLine("GET /trackFile?albumId={albumId}")
    fun getTrackFiles(@Param("albumId") albumId: Long): List<TrackFile>
}