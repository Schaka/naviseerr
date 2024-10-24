package com.github.schaka.naviseerr.music_library.library.release

import com.github.schaka.naviseerr.music_library.library.LibraryItemState

data class LibraryRelease(
    val id: Long? = null,
    val lidarrId: Long,
    val hash: Int,
    val name: String,
    val musicbrainzId: String,
    val type: String,
    val path: String,
    val state: LibraryItemState
)
