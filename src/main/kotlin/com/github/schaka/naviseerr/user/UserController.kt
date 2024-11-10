package com.github.schaka.naviseerr.user

import com.github.schaka.naviseerr.navidrome.NavidromeSessionUser
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userRepository: UserRepository
) {

    @GetMapping("")
    fun getUser(@AuthenticationPrincipal user: NavidromeSessionUser): UserData {
        return userRepository.getUser(user.id)
    }

    @PutMapping("")
    fun updateUser(@AuthenticationPrincipal user: NavidromeSessionUser, @RequestBody userData: UserData): UserData {
        return userRepository.updateUser(userData.copy(navidromeId = user.id))
    }
}