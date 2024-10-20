package com.github.schaka.naviseerr.servarr.radarr

import com.github.schaka.naviseerr.servarr.data_structures.Tag
import com.github.schaka.naviseerr.servarr.history.HistoryResponse
import com.github.schaka.naviseerr.servarr.quality_profile.QualityProfile
import com.github.schaka.naviseerr.servarr.radarr.movie.MovieFile
import com.github.schaka.naviseerr.servarr.radarr.movie.MoviePayload
import feign.Param
import feign.RequestLine

interface RadarrClient {

    @RequestLine("GET /history/movie?movieId={movieId}")
    fun getHistory(@Param("movieId") movieId: Int): List<HistoryResponse>

    @RequestLine("GET /movie")
    fun getAllMovies(): List<MoviePayload>

    @RequestLine("GET /moviefile?movieId={id}")
    fun getMovieFiles(@Param("id") id: Int): List<MovieFile>

    @RequestLine("GET /tag")
    fun getAllTags(): List<Tag>

    @RequestLine("GET /movie/{id}")
    fun getMovie(@Param("id") id: Int): MoviePayload

    @RequestLine("PUT /movie/{id}")
    fun updateMovie(@Param("id") id: Int, payload: MoviePayload)

    @RequestLine("DELETE /movie/{id}?deleteFiles={deleteFiles}")
    fun deleteMovie(@Param("id") id: Int, @Param("deleteFiles") deleteFiles: Boolean = true)

    @RequestLine("DELETE /moviefile/{id}")
    fun deleteMovieFile(@Param("id") id: Int)

    @RequestLine("GET /qualityprofile")
    fun getAllQualityProfiles(): List<QualityProfile>
}