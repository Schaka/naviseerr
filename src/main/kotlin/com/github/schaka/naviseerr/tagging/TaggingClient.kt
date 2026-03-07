package com.github.schaka.naviseerr.tagging

import feign.RequestLine

interface TaggingClient {

    @RequestLine("POST /api/v1/onetagger/tag")
    fun oneTaggerTag(request: TaggingRequest): TaggingResponse

    @RequestLine("POST /api/v1/fingerprint")
    fun fingerprint(request: TaggingRequest): TaggingResponse

    @RequestLine("POST /api/v1/tag")
    fun tag(request: TaggingRequest): TaggingResponse
}
