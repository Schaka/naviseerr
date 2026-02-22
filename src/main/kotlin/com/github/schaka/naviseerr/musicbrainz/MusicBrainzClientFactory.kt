package com.github.schaka.naviseerr.musicbrainz

import com.github.schaka.naviseerr.config.DefaultClientProperties
import feign.Feign
import feign.Request
import feign.jackson3.Jackson3Decoder
import feign.jackson3.Jackson3Encoder
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.util.concurrent.ConcurrentHashMap

@Component
class MusicBrainzClientFactory(
    private val defaults: DefaultClientProperties,
    private val properties: MusicBrainzProperties,
    private val mapper: JsonMapper,
) {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    private val clients = ConcurrentHashMap<String, MusicBrainzClient>()

    fun getOrCreateClient(username: String): MusicBrainzClient {
        return clients.getOrPut(username) {
            log.debug("Creating new MusicBrainz client for user: {}", username)
            createClient(username)
        }
    }

    private fun createClient(username: String): MusicBrainzClient {
        return Feign.builder()
            .options(Request.Options(defaults.connectTimeout, defaults.readTimeout, true))
            .decoder(Jackson3Decoder(mapper))
            .encoder(Jackson3Encoder(mapper))
            .requestInterceptor {
                it.header("User-Agent", "Naviseerr/1.0 (user: $username)")
                it.header("Accept", APPLICATION_JSON_VALUE)
                it.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            }
            .target(MusicBrainzClient::class.java, properties.url)
    }
}
