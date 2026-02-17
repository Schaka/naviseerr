package com.github.schaka.naviseerr.lastfm.nokey

import com.github.schaka.naviseerr.config.DefaultClientProperties
import com.github.schaka.naviseerr.lastfm.LastFMProperties
import feign.Feign
import feign.Request
import feign.jaxb.JAXBContextFactory
import feign.jaxb.JAXBDecoder
import feign.jaxb.JAXBEncoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for Last.fm public API client (unauthenticated calls).
 * Used during the authentication flow before a user session key is obtained.
 */
@Configuration
class LastFMPublicClientConfig(
    private val defaults: DefaultClientProperties,
    private val properties: LastFMProperties,
) {

    companion object {
        private val jaxbContextFactory = JAXBContextFactory.Builder()
            .withMarshallerJAXBEncoding("UTF-8")
            .build()
    }

    @Bean
    fun lastFmPublicApiClient(): LastFMPublicApiClient {
        return Feign.builder()
            .options(Request.Options(defaults.connectTimeout, defaults.readTimeout, true))
            .decoder(JAXBDecoder(jaxbContextFactory))
            .encoder(JAXBEncoder(jaxbContextFactory))
            .target(LastFMPublicApiClient::class.java, properties.url)
    }
}