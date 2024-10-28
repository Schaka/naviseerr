package com.github.schaka.naviseerr.download_client.slskd.dto

import java.util.UUID

data class Search(
    val searchText: String,
    val id: String = UUID.randomUUID().toString(),
)
