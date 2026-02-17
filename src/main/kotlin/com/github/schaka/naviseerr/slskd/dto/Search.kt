package com.github.schaka.naviseerr.slskd.dto

import java.util.UUID

data class Search(
    val searchText: String,
    val id: String = UUID.randomUUID().toString(),
)
