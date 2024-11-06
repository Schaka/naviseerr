package com.github.schaka.naviseerr.mediaserver

import feign.RequestLine

interface NavidromeClient {

    @RequestLine("GET /user")
    fun listUsers(): List<Map<Object, Object>>

}