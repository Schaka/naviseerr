package com.github.schaka.naviseerr.musicbrainz

import com.github.schaka.naviseerr.musicbrainz.dto.MusicBrainzArtistSearchResult
import com.github.schaka.naviseerr.musicbrainz.dto.MusicBrainzReleaseGroupSearchResult
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class MusicBrainzService(
    private val clientFactory: MusicBrainzClientFactory,
) {

    private val lastRequestTimes = ConcurrentHashMap<String, Long>()

    fun searchArtists(username: String, query: String, limit: Int = 25, offset: Int = 0): MusicBrainzArtistSearchResult {
        return rateLimited(username) {
            clientFactory.getOrCreateClient(username).searchArtists(query, limit, offset)
        }
    }

    fun searchReleaseGroups(username: String, query: String, limit: Int = 25, offset: Int = 0): MusicBrainzReleaseGroupSearchResult {
        return rateLimited(username) {
            clientFactory.getOrCreateClient(username).searchReleaseGroups(query, limit, offset)
        }
    }

    fun getReleaseGroupsByArtist(username: String, artistId: String, type: String = "album"): MusicBrainzReleaseGroupSearchResult {
        return rateLimited(username) {
            clientFactory.getOrCreateClient(username).getReleaseGroupsByArtist(artistId, type)
        }
    }

    private fun <T> rateLimited(username: String, block: () -> T): T {
        val lastRequest = lastRequestTimes[username] ?: 0
        val elapsed = System.currentTimeMillis() - lastRequest
        if (elapsed < 1100) {
            Thread.sleep(1100 - elapsed)
        }
        lastRequestTimes[username] = System.currentTimeMillis()
        return block()
    }
}
