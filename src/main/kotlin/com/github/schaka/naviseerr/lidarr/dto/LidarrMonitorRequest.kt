package com.github.schaka.naviseerr.lidarr.dto

data class LidarrMonitorRequest(
    val albumIds: List<Long>,
    val monitored: Boolean = true
)
