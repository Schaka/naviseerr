package com.github.schaka.naviseerr.listenbrainz

import com.github.schaka.naviseerr.config.DefaultClientProperties
import com.github.schaka.naviseerr.config.PerUserClientFactory
import com.github.schaka.naviseerr.navidrome.polling.TokenBasedClient
import com.github.schaka.naviseerr.db.user.NaviseerrUser
import feign.Feign
import feign.Request
import feign.jackson3.Jackson3Decoder
import feign.jackson3.Jackson3Encoder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.util.UUID
import kotlin.jvm.java

@Component
class ListenBrainzClientFactory(
    @Autowired private val defaults: DefaultClientProperties,
    @Autowired private val properties: ListenBrainzProperties,
    @Autowired private val objectMapper: JsonMapper
) : PerUserClientFactory<ListenBrainzApiClient> {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    private val clients = HashMap<UUID, TokenBasedClient<ListenBrainzApiClient>>()

    override fun getOrCreateClient(user: NaviseerrUser): ListenBrainzApiClient {
        val cached = clients[user.id]

        val userToken = user.listenBrainzToken ?: throw IllegalStateException("User ${user.username} has no ListenBrainz token")

        if (cached != null && cached.token == userToken) {
            return cached.client
        }

        log.debug("Creating new ListenBrainz client for user: {}", user.username)
        val newClient = createClient(userToken)
        clients[user.id] = TokenBasedClient(userToken, newClient)
        return newClient
    }

    override fun invalidateClient(userId: UUID) {
        clients.remove(userId)
        log.debug("Invalidated ListenBrainz client for user: {}", userId)
    }

    override fun invalidateAll() {
        clients.clear()
        log.debug("Invalidated all ListenBrainz clients")
    }

    private fun createClient(userToken: String): ListenBrainzApiClient {
        return Feign.builder()
            .options(Request.Options(defaults.connectTimeout, defaults.readTimeout, true))
            .decoder(Jackson3Decoder(objectMapper))
            .encoder(Jackson3Encoder(objectMapper))
            .requestInterceptor {
                it.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                it.header(HttpHeaders.AUTHORIZATION, "Token $userToken")
            }
            .target(ListenBrainzApiClient::class.java, properties.url)
    }
}
