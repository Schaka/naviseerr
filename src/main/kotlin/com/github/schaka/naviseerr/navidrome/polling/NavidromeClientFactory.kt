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
class NavidromeClientFactory(
    @Autowired private val defaults: DefaultClientProperties,
    @Autowired private val properties: NavidromeProperties,
    @Autowired private val mapper: JsonMapper,
) : PerUserClientFactory<NavidromeApiClient> {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    private val clients = HashMap<UUID, TokenBasedClient<NavidromeApiClient>>()

    override fun getOrCreateClient(user: NaviseerrUser): NavidromeApiClient {
        val cached = clients[user.id]

        if (cached != null && cached.token == user.navidromeToken) {
            return cached.client
        }

        log.debug("Creating new Navidrome API client for user: {}", user.username)
        val newClient = createClient(user)
        clients[user.id] = TokenBasedClient(user.navidromeToken, newClient)
        return newClient
    }

    override fun invalidateClient(userId: UUID) {
        clients.remove(userId)
        log.debug("Invalidated Navidrome API client for user: {}", userId)
    }

    override fun invalidateAll() {
        clients.clear()
        log.debug("Invalidated all Navidrome API clients")
    }

    private fun createClient(user: NaviseerrUser): NavidromeApiClient {
        return Feign.builder()
            .options(Request.Options(defaults.connectTimeout, defaults.readTimeout, true))
            .decoder(Jackson3Decoder(mapper))
            .encoder(Jackson3Encoder(mapper))
            .requestInterceptor {
                it.header("X-ND-Authorization", "Bearer ${user.navidromeToken}")
                it.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            }
            .target(NavidromeApiClient::class.java, "${properties.url}/api")
    }
}