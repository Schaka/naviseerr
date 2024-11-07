package com.github.schaka.naviseerr.config

import com.github.schaka.naviseerr.navidrome.NavidromeAuthenticationManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class WebSecurityConfig(
    private val authenticationManager: NavidromeAuthenticationManager
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .authenticationManager(authenticationManager)
            .authorizeHttpRequests {
                it
                    .requestMatchers("/login", "/login-error").permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin {
                it
                    .loginPage("/login")
                    .failureUrl("/login-error")
                    .defaultSuccessUrl("/dashboard", true)
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
}