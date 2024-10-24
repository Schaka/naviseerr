package com.github.schaka.naviseerr.music_library.lidarr

import com.github.schaka.naviseerr.music_library.lidarr.dto.LidarrAlbum
import com.github.schaka.naviseerr.music_library.lidarr.dto.LidarrArtist
import com.github.schaka.naviseerr.music_library.lidarr.dto.TrackFile
import feign.Param
import feign.RequestLine

interface LidarrClient {

    @RequestLine("GET /artist")
    fun getAllArtists(): List<LidarrArtist>

    @RequestLine("GET /album?artistId={artistId}&includeAllArtistAlbums=true")
    fun getAlbums(@Param("artistId") artistId: Long): List<LidarrAlbum>

    @RequestLine("GET /album?includeAllArtistAlbums=true")
    fun getAllAlbums(): List<LidarrAlbum>

    @RequestLine("GET /trackFile?albumId={albumId}")
    fun getTrackFiles(@Param("albumId") albumId: Long): List<TrackFile>
}