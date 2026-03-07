package com.github.schaka.naviseerr.tagging

data class TaggingResponse(
    val success: Boolean,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long
)
