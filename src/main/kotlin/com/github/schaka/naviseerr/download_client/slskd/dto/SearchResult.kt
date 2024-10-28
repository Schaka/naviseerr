package com.github.schaka.naviseerr.download_client.slskd.dto

data class SearchResult(
    val hasFreeUploadSlot: Boolean,
    val uploadSpeed: Long,
    val token: Long,
    val files: List<File>
)