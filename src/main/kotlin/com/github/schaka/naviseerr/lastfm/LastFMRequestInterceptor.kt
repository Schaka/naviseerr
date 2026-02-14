package com.github.schaka.naviseerr.lastfm

import feign.Request
import feign.RequestInterceptor
import feign.RequestTemplate
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Feign request interceptor that automatically adds Last.fm authentication to every request.
 *
 * For POST requests:
 * - Moves all query parameters to the request body as application/x-www-form-urlencoded
 * - Adds api_key, sk, and api_sig to the body
 * - Sets Content-Type header to application/x-www-form-urlencoded
 *
 * For GET requests:
 * - Adds api_key, sk, and api_sig as query parameters
 *
 * This allows developers to simply define Feign methods without worrying about authentication.
 */
class LastFMRequestInterceptor(
    private val apiKey: String,
    private val sessionKey: String,
    private val sharedSecret: String
) : RequestInterceptor {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

    }

    override fun apply(template: RequestTemplate) {
        val params = mutableMapOf<String, String>()

        template.queries().forEach { (key, values) ->
            if (values.isNotEmpty()) {
                val decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8)
                val decodedValue = URLDecoder.decode(values.first(), StandardCharsets.UTF_8)
                params[decodedKey] = decodedValue
            }
        }

        params["api_key"] = apiKey
        params["sk"] = sessionKey

        val signature = LastFMSignatureUtil.generateSignature(params, sharedSecret)
        params["api_sig"] = signature

        if (template.method() == Request.HttpMethod.POST.name) {
            template.queries(emptyMap<String, List<String>>()) // can't call clear on unmodifiableMap

            val formBody = params.entries
                .sortedBy { it.key }
                .joinToString("&") { (key, value) ->
                    "${formEncode(key)}=${formEncode(value)}"
                }

            template.body(formBody.toByteArray(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
            template.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        } else {
            template.query("api_key", apiKey)
            template.query("sk", sessionKey)
            template.query("api_sig", signature)
        }
    }

    private fun formEncode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
    }
}
