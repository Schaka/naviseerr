package com.github.schaka.naviseerr.music_library.lidarr

import feign.Param
import feign.RequestLine

interface LidarrClient {

    @RequestLine("GET /history/movie?movieId={movieId}")
    fun getHistory(@Param("movieId") movieId: Int): List<String>

}