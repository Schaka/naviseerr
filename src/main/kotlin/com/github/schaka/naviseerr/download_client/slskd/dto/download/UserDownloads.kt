package com.github.schaka.naviseerr.download_client.slskd.dto.download

data class UserDownloads(
    val username: String,
    val directories: List<UserDirectory>
)
