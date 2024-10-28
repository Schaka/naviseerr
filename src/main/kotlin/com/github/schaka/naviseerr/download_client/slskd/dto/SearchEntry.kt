package com.github.schaka.naviseerr.download_client.slskd.dto

data class SearchEntry(
    val searchText: String,
    val id: String,
    val state: String,
    val isComplete: Boolean
)
