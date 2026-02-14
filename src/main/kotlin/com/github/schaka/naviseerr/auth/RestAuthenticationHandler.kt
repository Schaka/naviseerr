package com.github.schaka.naviseerr.auth

import com.github.schaka.naviseerr.db.user.NaviseerrUser
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class RestAuthenticationHandler(
    private val objectMapper: JsonMapper,
) : AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private val securityContextRepository: SecurityContextRepository = HttpSessionSecurityContextRepository()

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)
        securityContextRepository.saveContext(context, request, response)

        response.status = HttpServletResponse.SC_OK
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        val principal = authentication.principal as NaviseerrUser

        val body = mapOf(
            "username" to principal.username,
            "roles" to authentication.authorities.map { it.authority }
        )

        objectMapper.writeValue(response.outputStream, body)
    }

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        val body = mapOf(
            "error" to "Authentication failed"
        )

        objectMapper.writeValue(response.outputStream, body)
    }
}
