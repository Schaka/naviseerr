package com.github.schaka.naviseerr.auth

import com.github.schaka.naviseerr.navidrome.auth.NavidromeAuthenticationManager
import com.github.schaka.naviseerr.db.user.NaviseerrUser
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView
import tools.jackson.databind.json.JsonMapper

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val authenticationManager: NavidromeAuthenticationManager,
    private val authenticationHandler: RestAuthenticationHandler,
    private val objectMapper: JsonMapper
) {

    @Bean
    fun securityContextRepository(): SecurityContextRepository {
        return HttpSessionSecurityContextRepository()
    }

    @Bean
    fun filterChain(
        http: HttpSecurity,
    ): SecurityFilterChain {

        val jsonAuthFilter = JsonUsernamePasswordAuthenticationFilter(objectMapper).apply {
            setAuthenticationManager(authenticationManager)
            setAuthenticationSuccessHandler(authenticationHandler)
            setAuthenticationFailureHandler(authenticationHandler)
            setFilterProcessesUrl("/api/auth/login")
        }

        http
            .csrf { it.disable() }
            .authenticationManager ( authenticationManager )
            .securityContext { it.securityContextRepository(securityContextRepository()) }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            }
            .authorizeHttpRequests {
                it
                    .requestMatchers("/api/auth/login").permitAll()
                    .requestMatchers("/index.html", "/error").permitAll()
                    .requestMatchers("/login").permitAll()
                    .requestMatchers("/assets/**", "/favicon.ico").permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterAt(jsonAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .logout {
                it.logoutUrl("/api/auth/logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler { _, response, _ ->
                        response.status = HttpServletResponse.SC_OK
                    }
            }
            .exceptionHandling {
                it.authenticationEntryPoint(DualEntryPoint(objectMapper))
            }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }

        return http.build()
    }
}

private class DualEntryPoint(private val objectMapper: JsonMapper) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        if (request.requestURI.startsWith("/api/")) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            objectMapper.writeValue(response.outputStream, mapOf("error" to "Unauthorized"))
        } else {
            response.sendRedirect("/login")
        }
    }
}

@Controller
class LoginController {

    // used to serve the correct route inside our VueJS application
    @GetMapping("/login")
    fun login(@AuthenticationPrincipal principal: Any): ModelAndView {
        if (principal is NaviseerrUser) {
            return ModelAndView("redirect:/")
        }
        return ModelAndView("index.html")
    }
}
