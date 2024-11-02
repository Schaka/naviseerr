package com.github.schaka.naviseerr.music_library.lidarr

import com.github.schaka.naviseerr.music_library.lidarr.dto.*
import com.github.schaka.naviseerr.music_library.lidarr.dto.quality.QualityDefinition
import feign.Param
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Path

@RegisterReflectionForBinding(
    classes = [
        LidarrArtist::class, LidarrAlbum::class, LidarrTrack::class, TrackFile::class,
        LidarrPage::class, WantedRecord::class, ImportCommand::class, QualityDefinition::class
    ]
)
@Service
@Transactional
class LidarrRestService(
    private val lidarrClient: LidarrClient,
) {

    fun getLibrary(): List<LidarrArtist> {
        return lidarrClient.getAllArtists().map { artist ->
            val albums = lidarrClient.getAlbums(artist.id).map { album ->
                val tracks = lidarrClient.getTrackFiles(album.id)
                //FIXME: if empty, get filenaming structure from Lidarr and build folder name from it
                val trackPath = tracks.firstOrNull() ?: TrackFile(-1, Path.of(artist.path).resolve("dummy-album/dummy-track.flac").toString())
                val path = Path.of(trackPath.path)
                album.path = path.parent.toString()

                return@map album
            }
            artist.albums += albums
            return@map artist
        }
    }

    fun getMissing(pageable: Pageable = PageRequest.of(0, 10)): LidarrPage<WantedRecord> {
        return lidarrClient.getMissing(pageable)
    }

    fun getTracks(albumId: Long): List<LidarrTrack> {
        return lidarrClient.getTracks(albumId)
    }
}