package com.github.schaka.naviseerr.lidarr.dto

data class ImportCommand(
    val files: List<ImportTrack>,
    val name: String = "ManualImport",
    val importMode: String = "move",
    val replaceExistingFiles: Boolean = false
)
