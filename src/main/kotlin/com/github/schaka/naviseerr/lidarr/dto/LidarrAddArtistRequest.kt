package com.github.schaka.naviseerr.lidarr.dto

data class LidarrAddArtistRequest(
    val foreignArtistId: String,
    val artistName: String,
    val qualityProfileId: Int,
    val metadataProfileId: Int,
    val rootFolderPath: String,
    val monitored: Boolean = true,
    val monitorNewItems: String = "all",
    val addOptions: AddArtistOptions = AddArtistOptions()
)

data class AddArtistOptions(
    val monitor: String = "all",
    val searchForMissingAlbums: Boolean = true
)
