package com.github.schaka.naviseerr.navidrome.polling

import com.github.schaka.naviseerr.config.DefaultClientProperties
import com.github.schaka.naviseerr.config.PerUserClientFactory
import com.github.schaka.naviseerr.navidrome.NavidromeProperties
import com.github.schaka.naviseerr.db.user.NaviseerrUser
import feign.Feign
import feign.Request
import feign.jackson3.Jackson3Decoder
import feign.jackson3.Jackson3Encoder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.util.UUID
import kotlin.jvm.java

@Component
class SubsonicClientFactory(
    @Autowired private val defaults: DefaultClientProperties,
    @Autowired private val properties: NavidromeProperties,
    @Autowired private val mapper: JsonMapper,
) : PerUserClientFactory<NavidromeSubsonicClient> {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    private val clients = HashMap<UUID, TokenBasedClient<NavidromeSubsonicClient>>()

    override fun getOrCreateClient(user: NaviseerrUser): NavidromeSubsonicClient {
        val cached = clients[user.id]

        if (cached != null && cached.token == user.subsonicToken) {
            return cached.client
        }

        log.debug("Creating new Subsonic client for user: {}", user.username)
        val newClient = createClient(user)
        clients[user.id] = TokenBasedClient(user.subsonicToken, newClient)
        return newClient
    }

    override fun invalidateClient(userId: UUID) {
        clients.remove(userId)
        log.debug("Invalidated Subsonic client for user: {}", userId)
    }

    override fun invalidateAll() {
        clients.clear()
        log.debug("Invalidated all Subsonic clients")
    }

    private fun createClient(user: NaviseerrUser): NavidromeSubsonicClient {
        return Feign.builder()
            .options(Request.Options(defaults.connectTimeout, defaults.readTimeout, true))
            .decoder(Jackson3Decoder(mapper))
            .encoder(Jackson3Encoder(mapper))
            .requestInterceptor {
                it.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                it.query("c", "Naviseerr")
                it.query("u", user.username)
                it.query("t", user.subsonicToken)
                it.query("s", user.subsonicSalt)
                it.query("v", "1.16.1")
                it.query("f", "json")
            }
            .target(NavidromeSubsonicClient::class.java, "${properties.url}/rest")
    }
}