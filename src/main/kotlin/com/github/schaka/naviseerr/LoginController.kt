package com.github.schaka.naviseerr

import com.github.schaka.naviseerr.navidrome.NavidromeSessionUser
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class LoginController {

    @GetMapping("/login")
    fun showLogin(model: Model): String {
        return "login"
    }

    @GetMapping("/dashboard")
    fun showLogin(@AuthenticationPrincipal user: NavidromeSessionUser, model: Model): String {
        model.addAttribute("user", user)
        return "dashboard"
    }
}