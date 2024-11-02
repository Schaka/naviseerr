package com.github.schaka.naviseerr.music_library.library

import com.github.schaka.naviseerr.download_client.slskd.SoulseekRestService
import com.github.schaka.naviseerr.download_client.slskd.MatchService
import com.github.schaka.naviseerr.music_library.lidarr.ImportManager
import com.github.schaka.naviseerr.music_library.lidarr.LidarrRestService
import com.github.schaka.naviseerr.music_library.lidarr.dto.WantedRecord
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component


@Component
class DownloadManager(
    val lidarrRestService: LidarrRestService,
    val soulseekRestService: SoulseekRestService,
    val matchService: MatchService,
    val importManager: ImportManager,
) {

    private val log = KotlinLogging.logger { }

    suspend fun downloadRelease(missing: WantedRecord) {
        val searchText = "${missing.artist.artistName} - ${missing.title}"
        val result = soulseekRestService.search(searchText)
        val expectedTracks = lidarrRestService.getTracks(missing.id)

        val matches = matchService.matchResultToTrackList(result, expectedTracks, missing.title, missing.artist.artistName)
            .sortedByDescending { it.score }

        if (matches.isNotEmpty()) {
            val match = matches.first()
            log.info { "Found album ${missing.title} - downloading from ${match.result.username}" }
            val downloadFiles = soulseekRestService.download(match)
            importManager.import(missing.artistId, missing.id, missing.releases.first().id, match)

        } else {
            log.info { "Couldn't find any results for ${missing.artist} - ${missing.title}" }
        }

    }

}