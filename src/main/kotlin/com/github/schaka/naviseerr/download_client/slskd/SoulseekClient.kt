package com.github.schaka.naviseerr.download_client.slskd

import com.github.schaka.naviseerr.download_client.slskd.dto.Search
import com.github.schaka.naviseerr.download_client.slskd.dto.SearchEntry
import com.github.schaka.naviseerr.download_client.slskd.dto.SearchResult
import feign.Param
import feign.RequestLine

interface SoulseekClient {

    @RequestLine("GET /downloads")
    fun downloads(): List<Map<Object, Object>>

    @RequestLine("GET /searches")
    fun listSearches(): List<SearchEntry>

    @RequestLine("GET /searches/{id}?includeResponses=true")
    fun searches(@Param("id") id: String): SearchEntry

    @RequestLine("GET /searches/{id}/responses")
    fun searchContent(@Param("id") id: String): List<SearchResult>

    @RequestLine("POST /searches")
    fun searchForMedia(search: Search)

}