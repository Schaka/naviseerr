package com.github.schaka.naviseerr.slskd

import com.github.schaka.naviseerr.slskd.dto.download.DownloadRequest
import com.github.schaka.naviseerr.slskd.dto.Search
import com.github.schaka.naviseerr.slskd.dto.SearchEntry
import com.github.schaka.naviseerr.slskd.dto.SearchResult
import com.github.schaka.naviseerr.slskd.dto.download.UserDownloads
import feign.Param
import feign.RequestLine

interface SoulseekClient {

    @RequestLine("GET /downloads")
    fun downloads(): List<Map<Any, Any>>

    @RequestLine("GET /searches")
    fun listSearches(): List<SearchEntry>

    @RequestLine("GET /searches/{id}?includeResponses=true")
    fun searches(@Param("id") id: String): SearchEntry

    @RequestLine("DELETE /searches/{id}")
    fun deleteSearch(@Param("id") id: String)

    @RequestLine("GET /searches/{id}/responses")
    fun searchContent(@Param("id") id: String): List<SearchResult>

    @RequestLine("POST /searches")
    fun searchForMedia(search: Search)

    @RequestLine("POST /transfers/downloads/{username}")
    fun startDownload(@Param("username") username: String, downloads: List<DownloadRequest>)

    @RequestLine("GET /transfers/downloads/{username}")
    fun getDownloads(@Param("username") username: String): UserDownloads

}