package com.github.schaka.naviseerr.music_library.library

import com.github.schaka.naviseerr.db.Tables.RELEASES
import com.github.schaka.naviseerr.music_library.library.artist.ArtistRepository
import com.github.schaka.naviseerr.music_library.library.artist.ReleaseRepository
import com.github.schaka.naviseerr.music_library.lidarr.LidarrRestService
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class LibraryManager(
    val lidarrRestService: LidarrRestService,
    val artistRepository: ArtistRepository,
    val releaseRepository: ReleaseRepository,
    val create: DSLContext
) {

    @Transactional
    fun updateLibrary() {
        // build internal library from Lidarr
        val artists = lidarrRestService.getLibrary()

        for (artist in artists) {

            val artistId = artistRepository.saveArtist(artist)
            releaseRepository.saveReleases(artist.albums, artistId)
        }
    }

    @Transactional(readOnly = true)
    fun getLidarrIdsForDownloadRun(): List<Long> {
        return create.select(RELEASES.LIDARR_ID)
            .from(RELEASES)
            .where(RELEASES.LAST_DOWNLOAD_ATTEMPT.le(LocalDateTime.now().minusDays(1)))
            .or(RELEASES.LAST_DOWNLOAD_ATTEMPT.isNull())
            .fetch()
            .map { it.component1() }
    }

    @Transactional
    fun updateLastDownload(toUpdate: List<Long>) {
        create.update(RELEASES)
            .set(RELEASES.LAST_DOWNLOAD_ATTEMPT, LocalDateTime.now())
            .where(RELEASES.LIDARR_ID.`in`(toUpdate))
            .execute()
    }
}