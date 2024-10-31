package com.github.schaka.naviseerr.music_library.library

import com.github.schaka.naviseerr.download_client.slskd.SoulseekRestService
import com.github.schaka.naviseerr.download_client.slskd.lucene.MatchService
import com.github.schaka.naviseerr.music_library.lidarr.LidarrRestService
import com.github.schaka.naviseerr.music_library.lidarr.dto.WantedRecord
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component


@Component
class DownloadManager(
    val lidarrRestService: LidarrRestService,
    val soulseekRestService: SoulseekRestService,
    val matchService: MatchService,
) {

    private val log = KotlinLogging.logger { }

    suspend fun downloadRelease(missing: WantedRecord) {
        val searchText = "${missing.artist.artistName} - ${missing.title}"
        val result = soulseekRestService.search(searchText)
        val expectedTracks = lidarrRestService.getTracks(missing.id)

        val match = matchService.matchResultToTrackList(result, expectedTracks, missing.title, missing.artist.artistName)
        log.info { match }
    }

}