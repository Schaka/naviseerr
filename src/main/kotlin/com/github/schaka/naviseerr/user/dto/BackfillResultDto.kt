package com.github.schaka.naviseerr.user.dto

data class BackfillResultDto(
    val success: Boolean,
    val message: String,
    val newPlaysCount: Int,
    val requestedCount: Int
)
