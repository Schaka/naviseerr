package com.github.schaka.naviseerr.lidarr

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.stereotype.Service

@Service
class LidarrConfigCache(private val lidarrClient: LidarrClient) : SmartInitializingSingleton {

    private val log = LoggerFactory.getLogger(javaClass)

    data class LidarrConfig(
        val rootFolderPath: String,
        val qualityProfileId: Int,
        val metadataProfileId: Int
    )

    private lateinit var config: LidarrConfig

    override fun afterSingletonsInstantiated() {
        val rootFolders = lidarrClient.getRootFolders()
        val qualityProfiles = lidarrClient.getQualityProfiles()
        val metadataProfiles = lidarrClient.getMetadataProfiles()

        config = LidarrConfig(
            rootFolderPath = rootFolders.first().path,
            qualityProfileId = qualityProfiles.first().id,
            metadataProfileId = metadataProfiles.first().id
        )

        log.info("Lidarr config loaded: rootFolder={}, qualityProfile={}, metadataProfile={}",
            config.rootFolderPath, config.qualityProfileId, config.metadataProfileId)
    }

    fun getConfig(): LidarrConfig = config
}
