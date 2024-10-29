package com.github.schaka.naviseerr.music_library.lidarr

import com.github.schaka.naviseerr.music_library.lidarr.dto.LidarrAlbum
import com.github.schaka.naviseerr.music_library.lidarr.dto.LidarrArtist
import com.github.schaka.naviseerr.music_library.lidarr.dto.LidarrPage
import com.github.schaka.naviseerr.music_library.lidarr.dto.LidarrTrack
import com.github.schaka.naviseerr.music_library.lidarr.dto.TrackFile
import com.github.schaka.naviseerr.music_library.lidarr.dto.WantedRecord
import feign.Param
import feign.RequestLine
import org.springframework.data.domain.Pageable

interface LidarrClient {

    @RequestLine("GET /artist")
    fun getAllArtists(): List<LidarrArtist>

    @RequestLine("GET /album?artistId={artistId}&includeAllArtistAlbums=true")
    fun getAlbums(@Param("artistId") artistId: Long): List<LidarrAlbum>

    @RequestLine("GET /album?includeAllArtistAlbums=true")
    fun getAllAlbums(): List<LidarrAlbum>

    @RequestLine("GET /track?albumId={albumId}")
    fun getTracks(@Param("albumId") albumId: Long): List<LidarrTrack>

    @RequestLine("GET /trackFile?albumId={albumId}")
    fun getTrackFiles(@Param("albumId") albumId: Long): List<TrackFile>

    @RequestLine("GET /wanted/missing?includeArtist=true&monitored=true")
    fun getMissing(pageable: Pageable): LidarrPage<WantedRecord>
}