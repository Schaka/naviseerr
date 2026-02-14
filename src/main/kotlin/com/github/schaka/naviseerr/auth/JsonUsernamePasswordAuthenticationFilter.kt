package com.github.schaka.naviseerr.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import tools.jackson.databind.json.JsonMapper
import kotlin.jvm.java

class JsonUsernamePasswordAuthenticationFilter(
    private val objectMapper: JsonMapper
) : UsernamePasswordAuthenticationFilter() {

    override fun attemptAuthentication(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): Authentication {

        if (request.contentType != MediaType.APPLICATION_JSON_VALUE) {
            throw AuthenticationServiceException("Unsupported Content-Type")
        }

        val loginRequest = objectMapper.readValue(
            request.inputStream,
            LoginRequest::class.java
        )

        val authRequest = UsernamePasswordAuthenticationToken(
            loginRequest.username,
            loginRequest.password
        )

        return authenticationManager.authenticate(authRequest)
    }
}
