package com.github.schaka.naviseerr.music_library.lidarr

import com.github.schaka.naviseerr.download_client.slskd.SoulseekProperties
import com.github.schaka.naviseerr.download_client.slskd.dto.SearchMatchResult
import com.github.schaka.naviseerr.music_library.lidarr.dto.ImportCommand
import com.github.schaka.naviseerr.music_library.lidarr.dto.ImportTrack
import com.github.schaka.naviseerr.music_library.lidarr.dto.quality.QualityDefinition
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.nio.file.Path

@Component
class ImportManager(
    private val soulseekProperties: SoulseekProperties,
    private val lidarrProperties: LidarrProperties,
    private val lidarrClient:LidarrClient,
    private var qualities: List<QualityDefinition> = listOf()
) {

    init {
        qualities = lidarrClient.getQualityDefinitions()
    }

    fun import(artistId: Long, albumId: Long, albumReleaseId: Long, match: SearchMatchResult) {

        val albumDirectory = soulseekProperties.downloadDir
        val quality = qualities.filter { it.quality.name == "FLAC" }.first()
        val tracks = match.tracks.map { ImportTrack(resolveDir(albumDirectory, it.file!!), artistId, albumId, listOf(it.lidarrTrack.id), quality, albumReleaseId) }

        lidarrClient.import(ImportCommand(tracks))
    }

    private fun resolveDir(targetRoot: String, remoteFilePathAndName: String): String {
        val path = UriComponentsBuilder
            .fromPath(remoteFilePathAndName.replace("\\", "/"))
            .build()

        val segments = path.pathSegments.toMutableList()
        val fileNameAndExtension = segments.removeLast()
        val albumFolder = segments.removeLast()

        return Path.of(targetRoot).resolve(albumFolder, fileNameAndExtension).toString().replace("\\", "/")
    }
}