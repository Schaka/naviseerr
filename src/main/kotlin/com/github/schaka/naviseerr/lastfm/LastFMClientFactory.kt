package com.github.schaka.naviseerr.lastfm

import com.github.schaka.naviseerr.config.DefaultClientProperties
import com.github.schaka.naviseerr.config.PerUserClientFactory
import com.github.schaka.naviseerr.navidrome.polling.TokenBasedClient
import com.github.schaka.naviseerr.db.user.NaviseerrUser
import feign.Feign
import feign.Request
import feign.jaxb.JAXBContextFactory
import feign.jaxb.JAXBDecoder
import feign.jaxb.JAXBEncoder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.jvm.java

@Component
class LastFMClientFactory(
    @Autowired private val defaults: DefaultClientProperties,
    @Autowired private val properties: LastFMProperties,
) : PerUserClientFactory<LastFMApiClient> {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        private val jaxbContextFactory = JAXBContextFactory.Builder()
            .withMarshallerJAXBEncoding("UTF-8")
            .build()
    }

    private val clients = HashMap<UUID, TokenBasedClient<LastFMApiClient>>()

    override fun getOrCreateClient(user: NaviseerrUser): LastFMApiClient {
        val cached = clients[user.id]

        val sessionKey = user.lastFmSessionKey ?: throw IllegalStateException("User ${user.username} has no LastFM session key")

        if (cached != null && cached.token == sessionKey) {
            return cached.client
        }

        // Session key changed or no cached client - create new one
        log.debug("Creating new LastFM client for user: {}", user.username)
        val newClient = createClient(sessionKey)
        clients[user.id] = TokenBasedClient(sessionKey, newClient)
        return newClient
    }

    override fun invalidateClient(userId: UUID) {
        clients.remove(userId)
        log.debug("Invalidated LastFM client for user: {}", userId)
    }

    override fun invalidateAll() {
        clients.clear()
        log.debug("Invalidated all LastFM clients")
    }

    private fun createClient(sessionKey: String): LastFMApiClient {
        return Feign.builder()
            .options(Request.Options(defaults.connectTimeout, defaults.readTimeout, true))
            .decoder(JAXBDecoder(jaxbContextFactory))
            .encoder(JAXBEncoder(jaxbContextFactory))
            .requestInterceptor(LastFMRequestInterceptor(
                apiKey = properties.apiKey,
                sessionKey = sessionKey,
                sharedSecret = properties.sharedSecret
            ))
            .target(LastFMApiClient::class.java, properties.url)
    }
}