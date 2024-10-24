package com.github.schaka.naviseerr.music_library.lidarr

import com.github.schaka.naviseerr.music_library.lidarr.dto.LidarrAlbum
import com.github.schaka.naviseerr.music_library.lidarr.dto.LidarrArtist
import com.github.schaka.naviseerr.music_library.lidarr.dto.TrackFile
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Path

@RegisterReflectionForBinding(classes = [LidarrArtist::class, LidarrAlbum::class, TrackFile::class])
@Service
@Transactional
class LidarrRestService(
    val lidarrClient: LidarrClient,
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
}