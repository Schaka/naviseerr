package com.github.schaka.naviseerr.navidrome.auth

import com.github.schaka.naviseerr.navidrome.NavidromeProperties
import com.github.schaka.naviseerr.db.user.NaviseerrUser
import com.github.schaka.naviseerr.navidrome.polling.NavidromeSubsonicClient
import com.github.schaka.naviseerr.navidrome.polling.SubsonicClientFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.MessageDigest
import java.util.UUID

@Configuration
class NavidromeAdminClientConfig {

    companion object {
        const val SUBSONIC_ADMIN_CLIENT = "NavidromeSubsonicAdminClient"
    }

    @Qualifier(SUBSONIC_ADMIN_CLIENT)
    @Bean
    fun navidromeSubsonicAdminClient(navidromeProperties: NavidromeProperties, navidromeSubsonicClientFactory: SubsonicClientFactory): NavidromeSubsonicClient {
        val salt = UUID.randomUUID().toString().substring(0, 6)
        val token = md5(navidromeProperties.adminPass + salt)
        return navidromeSubsonicClientFactory.getOrCreateClient(
            NaviseerrUser(
                UUID.randomUUID(),
                navidromeProperties.adminUser,
                "fake-id",
                "fake-token",
                salt,
                token
            )
        )
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}