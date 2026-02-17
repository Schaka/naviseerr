package com.github.schaka.naviseerr.navidrome.polling

import com.github.schaka.naviseerr.navidrome.polling.dto.NavidromeSong
import feign.Param
import feign.RequestLine

/**
 * Feign client interface for Navidrome's custom REST API endpoints.
 */
interface NavidromeApiClient {

    /**
     * Get user activity/play history.
     * Endpoint: GET /api/activity
     */
    @RequestLine("GET /song?_end={size}&_order=DESC&_sort=play_date&_start=0")
    fun getActivity(
        @Param("size") size: Int = 100
    ): List<NavidromeSong>

}
