package com.github.schaka.naviseerr.musicbrainz

import com.github.schaka.naviseerr.musicbrainz.dto.MusicBrainzArtistSearchResult
import com.github.schaka.naviseerr.musicbrainz.dto.MusicBrainzReleaseGroupSearchResult
import feign.Param
import feign.RequestLine

interface MusicBrainzClient {

    @RequestLine("GET /artist?query={query}&fmt=json&limit={limit}&offset={offset}")
    fun searchArtists(
        @Param("query") query: String,
        @Param("limit") limit: Int = 25,
        @Param("offset") offset: Int = 0
    ): MusicBrainzArtistSearchResult

    @RequestLine("GET /release-group?query={query}&fmt=json&limit={limit}&offset={offset}")
    fun searchReleaseGroups(
        @Param("query") query: String,
        @Param("limit") limit: Int = 25,
        @Param("offset") offset: Int = 0
    ): MusicBrainzReleaseGroupSearchResult

    @RequestLine("GET /release-group?artist={artistId}&type={type}&fmt=json&limit={limit}&offset={offset}")
    fun getReleaseGroupsByArtist(
        @Param("artistId") artistId: String,
        @Param("type") type: String = "album",
        @Param("limit") limit: Int = 100,
        @Param("offset") offset: Int = 0
    ): MusicBrainzReleaseGroupSearchResult
}
