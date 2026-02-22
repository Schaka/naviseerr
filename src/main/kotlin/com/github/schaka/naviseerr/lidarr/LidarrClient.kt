package com.github.schaka.naviseerr.lidarr

import com.github.schaka.naviseerr.lidarr.dto.*
import com.github.schaka.naviseerr.lidarr.dto.quality.QualityDefinition
import feign.Param
import feign.RequestLine
import org.springframework.data.domain.Pageable

interface LidarrClient {

    @RequestLine("GET /artist")
    fun getAllArtists(): List<LidarrArtist>

    @RequestLine("GET /artist/lookup?term={term}")
    fun lookupArtist(@Param("term") term: String): List<LidarrArtist>

    @RequestLine("POST /artist")
    fun addArtist(request: LidarrAddArtistRequest): LidarrArtist

    @RequestLine("GET /album?artistId={artistId}&includeAllArtistAlbums=true")
    fun getAlbums(@Param("artistId") artistId: Long): List<LidarrAlbum>

    @RequestLine("GET /album?includeAllArtistAlbums=true")
    fun getAllAlbums(): List<LidarrAlbum>

    @RequestLine("GET /album/lookup?term={term}")
    fun lookupAlbum(@Param("term") term: String): List<LidarrAlbum>

    @RequestLine("POST /album")
    fun addAlbum(request: LidarrAddAlbumRequest): LidarrAlbum

    @RequestLine("PUT /album/monitor")
    fun monitorAlbums(request: LidarrMonitorRequest)

    @RequestLine("GET /track?albumId={albumId}")
    fun getTracks(@Param("albumId") albumId: Long): List<LidarrTrack>

    @RequestLine("GET /trackFile?albumId={albumId}")
    fun getTrackFiles(@Param("albumId") albumId: Long): List<TrackFile>

    @RequestLine("GET /wanted/missing?includeArtist=true&monitored=true")
    fun getMissing(pageable: Pageable): LidarrPage<WantedRecord>

    @RequestLine("POST /command")
    fun import(import: ImportCommand)

    @RequestLine("POST /command")
    fun searchAlbums(command: LidarrSearchCommand)

    @RequestLine("PUT /artist/{id}")
    fun updateArtist(@Param("id") id: Long, artist: LidarrArtist): LidarrArtist

    @RequestLine("GET /qualitydefinition")
    fun getQualityDefinitions(): List<QualityDefinition>

    @RequestLine("GET /queue?includeArtist=true&includeAlbum=true")
    fun getQueue(pageable: Pageable): LidarrPage<LidarrQueueItem>

    @RequestLine("GET /rootfolder")
    fun getRootFolders(): List<LidarrRootFolder>

    @RequestLine("GET /qualityprofile")
    fun getQualityProfiles(): List<LidarrQualityProfile>

    @RequestLine("GET /metadataprofile")
    fun getMetadataProfiles(): List<LidarrMetadataProfile>
}