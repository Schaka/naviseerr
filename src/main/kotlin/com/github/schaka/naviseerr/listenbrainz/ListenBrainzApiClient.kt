package com.github.schaka.naviseerr.listenbrainz

import com.github.schaka.naviseerr.listenbrainz.dto.SubmitListensRequest
import feign.RequestLine

/**
 * Feign client for ListenBrainz API calls.
 * API documentation: https://listenbrainz.readthedocs.io/en/production/dev/api/
 *
 * Authentication (Bearer token) is handled automatically by ListenBrainzRequestInterceptor.
 */
interface ListenBrainzApiClient {

    /**
     * Submit listens to ListenBrainz.
     * https://listenbrainz.readthedocs.io/en/production/dev/api/#post--1-submit-listens
     *
     * @param request The listen submission request
     */
    @RequestLine("POST /submit-listens")
    fun submitListens(request: SubmitListensRequest)
}
