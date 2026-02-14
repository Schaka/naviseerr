package com.github.schaka.naviseerr.navidrome.polling

data class TokenBasedClient<T>(
    val token: String,
    val client: T
)
