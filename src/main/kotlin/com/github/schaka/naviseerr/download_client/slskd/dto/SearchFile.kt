package com.github.schaka.naviseerr.download_client.slskd.dto

data class SearchFile(
    val filename: String,
    val size: Int,
    val extension: String,
    val length: Long?,
    val bitRate: Int?,
    val bitDepth: Int?,
    val sampleRate: Int?,
)
