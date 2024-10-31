package com.github.schaka.naviseerr.download_client.slskd.dto

data class SearchFile(
    val filename: String,
    val size: Int,
    val extension: String,
    val length: Long? = null,
    val bitRate: Int? = null,
    val bitDepth: Int? = null,
    val sampleRate: Int? = null,
)
