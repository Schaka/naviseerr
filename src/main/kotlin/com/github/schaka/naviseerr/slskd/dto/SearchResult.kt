package com.github.schaka.naviseerr.slskd.dto

data class SearchResult(
    val hasFreeUploadSlot: Boolean,
    val uploadSpeed: Long,
    val token: Long,
    val files: List<SearchFile>,
    val username: String,
)