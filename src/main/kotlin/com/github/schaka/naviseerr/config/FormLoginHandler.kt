package com.github.schaka.naviseerr.config

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class FormLoginHandler(
    private val mapper: ObjectMapper,
): AuthenticationSuccessHandler, AuthenticationFailureHandler {


    override fun onAuthenticationSuccess(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication) {
        response.status = HttpStatus.OK.value()
        response.contentType = APPLICATION_JSON_VALUE
        response.writer.write(mapper.writeValueAsString(LoginResponse("Successfully logged in")))
        response.writer.flush()
    }

    override fun onAuthenticationFailure(request: HttpServletRequest, response: HttpServletResponse, exception: AuthenticationException) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = APPLICATION_JSON_VALUE
        response.writer.write(mapper.writeValueAsString(LoginResponse(exception.message ?: "Failed to log in")))
        response.writer.flush()
    }

    private data class LoginResponse(
        val message: String,
    )

}