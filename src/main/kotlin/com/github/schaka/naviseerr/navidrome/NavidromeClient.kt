package com.github.schaka.janitorr.mediaserver

import feign.Param
import feign.RequestLine

interface NavidromeClient {

    @RequestLine("GET /Users")
    fun listUsers(): List<String>

}