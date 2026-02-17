package com.github.schaka.naviseerr.slskd.dto.download


data class UserDirectory(
    val directory: String,
    val fileCount: Int,
    val files: List<TransferFile>,
) {
    fun isComplete(): Boolean {
        return files.all { it.isComplete() }
    }
}
