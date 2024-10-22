package com.github.schaka.naviseerr.music_library.lidarr

import com.github.schaka.naviseerr.music_library.lidarr.dto.Album
import com.github.schaka.naviseerr.music_library.lidarr.dto.Artist
import com.github.schaka.naviseerr.music_library.lidarr.dto.TrackFile
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@RegisterReflectionForBinding(classes = [Artist::class, Album::class, TrackFile::class])
@Service
@Transactional
class LidarrRestService(
    val lidarrClient: LidarrClient,
) {
    fun getLibrary(): List<Artist> {
        return lidarrClient.getAllArtists().map {
            val albums = lidarrClient.getAlbums(it.id).map { album ->
                val tracks = lidarrClient.getTrackFiles(album.id)
                //FIXME: convert to path and use parent
                //if empty, get filenaming structure from Lidarr and build folder name from it
                album.path = tracks.firstOrNull()?.path
                return@map album
            }
            it.albums += albums
            return@map it
        }
    }
}