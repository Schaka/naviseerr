package com.github.schaka.naviseerr

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class LoginController {

    @GetMapping("/login")
    fun showLogin(model: Model): String {
        return "login"
    }
}