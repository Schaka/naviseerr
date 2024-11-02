package com.github.schaka.naviseerr.download_client.slskd.dto.download

data class TransferFile(
    val id: String,
    val direction: String,
    val filename: String,
    val state: String,
) {

    fun isComplete(): Boolean {
        return state == "Completed, Succeeded"
    }

}

