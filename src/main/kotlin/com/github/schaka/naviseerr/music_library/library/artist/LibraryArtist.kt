package com.github.schaka.naviseerr.music_library.library.artist

import com.github.schaka.naviseerr.music_library.library.LibraryItemState

data class LibraryArtist(
    val id: Long? = null,
    val lidarrId: Long,
    val hash: Int,
    val name: String,
    val musicbrainzId: String,
    val path: String,
    val state: LibraryItemState

)
