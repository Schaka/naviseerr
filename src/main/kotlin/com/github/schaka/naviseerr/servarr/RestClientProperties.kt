package com.github.schaka.naviseerr.servarr

interface RestClientProperties {
    val enabled: Boolean
    val url: String
    val apiKey: String
}