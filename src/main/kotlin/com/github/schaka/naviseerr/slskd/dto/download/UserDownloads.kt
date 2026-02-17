package com.github.schaka.naviseerr.slskd.dto.download

data class UserDownloads(
    val username: String,
    val directories: List<UserDirectory>
)
