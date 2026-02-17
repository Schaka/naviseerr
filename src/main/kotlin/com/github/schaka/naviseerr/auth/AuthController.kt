package com.github.schaka.naviseerr.auth

import com.github.schaka.naviseerr.db.user.NaviseerrUser
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.to

@RestController
@RequestMapping("/api/auth")
class AuthController {

    @GetMapping("/me")
    fun me(authentication: Authentication): Map<String, Any> {
        val principal = authentication.principal as NaviseerrUser
        return mapOf(
            "username" to principal.username,
            "roles" to authentication.authorities.map { it.authority }
        )
    }
}
