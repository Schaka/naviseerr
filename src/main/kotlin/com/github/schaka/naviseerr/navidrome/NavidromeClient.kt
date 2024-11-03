package com.github.schaka.naviseerr.mediaserver

import feign.RequestLine

interface NavidromeClient {

    @RequestLine("GET /Users")
    fun listUsers(): List<String>

}