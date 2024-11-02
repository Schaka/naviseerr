package com.github.schaka.naviseerr.music_library.lidarr

import com.github.schaka.naviseerr.download_client.slskd.SoulseekProperties
import com.github.schaka.naviseerr.download_client.slskd.dto.SearchFile
import com.github.schaka.naviseerr.download_client.slskd.dto.SearchMatchResult
import com.github.schaka.naviseerr.music_library.lidarr.dto.ImportCommand
import com.github.schaka.naviseerr.music_library.lidarr.dto.ImportTrack
import com.github.schaka.naviseerr.music_library.lidarr.dto.quality.QualityDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
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

    private val log = KotlinLogging.logger { }

    fun import(artistId: Long, albumId: Long, albumReleaseId: Long, match: SearchMatchResult) {

        val albumDirectory = soulseekProperties.downloadDir

        val tracks = match.tracks.map { ImportTrack(resolveDir(albumDirectory, it.file?.filename!!), artistId, albumId, listOf(it.lidarrTrack.id), resolveQuality(it.file), albumReleaseId) }

        log.debug { "Triggering manual import for download: $tracks" }
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

    private fun resolveQuality(result: SearchFile): QualityDefinition {
        val extension = getFileExtension(result.filename) ?: return qualities.first { it.quality.name == "Unknown" }

        val is24Bit = result.bitDepth == 24
        val isFlac = extension == "flac"
        if (isFlac) {
            val bitDepth = if (is24Bit) " 24bit" else ""
            return qualities.first { it.quality.name == "FLAC${bitDepth}" }
        }

        val missingBitrate = result.bitRate == null
        if (missingBitrate) {
            // fallback - unlikely to be lower
            return qualities.first { it.quality.name == "MP3-128" }
        }

        return qualities.first { it.quality.name == "MP3-${result.bitRate}" }
    }

    private fun getFileExtension(file: String): String? {
        val dotIndex = file.lastIndexOf('.')
        if (dotIndex == -1) {
            return file.substring(file.lastIndexOf('.') + 1).lowercase()
        }
        return null
    }
}