package com.github.schaka.naviseerr.music_library.library

import com.github.schaka.naviseerr.download_client.slskd.SoulseekRestService
import com.github.schaka.naviseerr.download_client.slskd.dto.SearchResult
import com.github.schaka.naviseerr.music_library.lidarr.LidarrRestService
import com.github.schaka.naviseerr.music_library.lidarr.dto.LidarrTrack
import com.github.schaka.naviseerr.music_library.lidarr.dto.WantedRecord
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.text.similarity.LevenshteinDistance
import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.math.abs
import kotlin.math.min

@Component
class DownloadManager(
    val lidarrRestService: LidarrRestService,
    val soulseekRestService: SoulseekRestService,
) {

    private val log = KotlinLogging.logger { }

    suspend fun downloadRelease(missing: WantedRecord) {
        val searchText = "${missing.artist.artistName} - ${missing.title}"
        val result = soulseekRestService.search(searchText)
        val expectedTracks = lidarrRestService.getTracks(missing.id)

        val match = result
            .filter{ it.files.isNotEmpty() }
            .sortedBy { score(it, expectedTracks, missing.artist.artistName) }
        log.info { match }
    }

    private fun score(result: SearchResult, tracks: List<LidarrTrack>, artistName: String): Double {
        var score: Double = 10.0
        if (!result.hasFreeUploadSlot) {
            // small punishment for not *currently* having an upload slot
            score -= 1
        }

        // minimal score for upload speed
        score+= result.uploadSpeed / 100_000

        val relevantFiles = result.files.filter { it.extension == "mp3" || it.extension == "flac" }
        score -= abs(relevantFiles.size - tracks.size)

        for (track in tracks) {
            score -= closenessScore(result, track, artistName)
        }

        return score

    }

    private fun closenessScore(result: SearchResult, track: LidarrTrack, artistName: String): Int {
        val lev = LevenshteinDistance.getDefaultInstance()
        val searchTrack = track.title.lowercase()
        val searchArtist = artistName.lowercase()

        return result.files.map { file ->
            //FIXME some minimum size or distance in relation to total string length needs to be accounted for
            val searchFilename = Path.of(file.filename.replace("@", "")).fileName.toString()
            val songDistance = lev.apply(searchFilename, searchTrack)
            val songArtistDistance = lev.apply(searchFilename, "$searchArtist - $searchTrack")
            min(songDistance, songArtistDistance)
        }.sorted().first()

    }

}