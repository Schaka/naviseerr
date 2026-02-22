package com.github.schaka.naviseerr.lidarr.dto

data class LidarrSearchCommand(val name: String, val albumIds: List<Long>? = null, val artistId: Long? = null)
