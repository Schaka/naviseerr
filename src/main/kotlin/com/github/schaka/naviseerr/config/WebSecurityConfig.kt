package com.github.schaka.naviseerr.config

import com.github.schaka.naviseerr.navidrome.NavidromeAuthenticationManager
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView

@Configuration
@EnableWebSecurity
class WebSecurityConfig(
    private val authenticationManager: NavidromeAuthenticationManager,
    private val formLoginHandler: FormLoginHandler,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .authenticationManager(authenticationManager)
            .authorizeHttpRequests {
                it
                    .requestMatchers(
                        antMatcher("/**/*.js"),
                        antMatcher("/**/*.css")
                    ).permitAll()
                    .requestMatchers(
                        antMatcher("/**/*.png"),
                        antMatcher("/**/*.jpg"),
                        antMatcher("/**/*.svg"),
                        antMatcher("/**/*.gif"),
                    ).permitAll()
                    .requestMatchers(
                        antMatcher("/**/*.woff"),
                        antMatcher("/**/*.woff2"),
                        antMatcher("/**/*.ttf"),
                        antMatcher("/**/*.otf"),
                    ).permitAll()
                    .requestMatchers(antMatcher("/**/favicon.ico")).permitAll()

                    .requestMatchers("/*.html", "/error").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().authenticated()
            }
            .formLogin {
                it
                    .loginPage("/login")
                    .loginProcessingUrl("/api/login")
                    .successHandler(formLoginHandler)
                    .failureHandler(formLoginHandler)
                    .permitAll()
            }
            .logout {
                it
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login")
                    .permitAll()
            }
            .build()
    }

    @Controller
    class LoginController {

        // used to serve the correct route inside our VueJS application
        @GetMapping("/login")
        fun login(): ModelAndView {
            return ModelAndView("index.html")
        }
    }

}