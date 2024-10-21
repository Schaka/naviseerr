package com.github.schaka.naviseerr.music_library

interface RestClientProperties {
    val enabled: Boolean
    val url: String
    val apiKey: String
}