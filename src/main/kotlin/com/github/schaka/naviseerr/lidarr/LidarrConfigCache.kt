package com.github.schaka.naviseerr.lidarr

import com.github.schaka.naviseerr.lidarr.dto.LidarrMetadataProfile
import com.github.schaka.naviseerr.lidarr.dto.LidarrQualityProfile
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.stereotype.Service

@Service
class LidarrConfigCache(
    private val lidarrClient: LidarrClient,
    private val lidarrProperties: LidarrProperties
) : SmartInitializingSingleton {

    private val log = LoggerFactory.getLogger(javaClass)

    data class LidarrConfig(
        val rootFolderPath: String,
        val qualityProfile: LidarrQualityProfile,
        val metadataProfile: LidarrMetadataProfile
    )

    private lateinit var config: LidarrConfig

    override fun afterSingletonsInstantiated() {
        val rootFolders = lidarrClient.getRootFolders()
        val qualityProfiles = lidarrClient.getQualityProfiles()
        val metadataProfiles = lidarrClient.getMetadataProfiles()

        config = LidarrConfig(
            rootFolderPath = rootFolders.first().path,
            qualityProfile = qualityProfiles.firstOrNull{ it.id == lidarrProperties.qualityProfileId } ?: qualityProfiles.first(),
            metadataProfile = metadataProfiles.firstOrNull{ it.id == lidarrProperties.metadataProfileId } ?: metadataProfiles.first(),
        )

        log.info("Lidarr config loaded: rootFolder={}, qualityProfile={}, metadataProfile={}",
            config.rootFolderPath, config.qualityProfile, config.metadataProfile)
    }

    fun getConfig(): LidarrConfig = config

    fun allowedReleaseTypes(): List<String> {
        return getConfig().metadataProfile.primarilyAlbumTypes.filter { it.allowed }.map { it.albumType.name }
    }
}
