package com.github.schaka.naviseerr.lastfm

import java.math.BigInteger
import java.security.MessageDigest

/**
 * Utility for generating Last.fm API signatures according to their specification.
 * Used by both the authentication service and the request interceptor.
 *
 * Last.fm signature generation:
 * 1. Sort parameters alphabetically by name
 * 2. Concatenate as name+value pairs (no separators)
 * 3. Append shared secret
 * 4. Calculate MD5 hash
 * 5. Convert to 32-character hex string
 */
object LastFMSignatureUtil {

    /**
     * Generates MD5 signature for Last.fm API calls.
     *
     * @param params All request parameters (including api_key, sk, method, etc.)
     * @param sharedSecret The application's shared secret
     * @return 32-character lowercase hex MD5 signature
     */
    fun generateSignature(params: Map<String, String>, sharedSecret: String): String {
        val sortedParams = params.toSortedMap()
        val concatenated = sortedParams.entries.joinToString("") { "${it.key}${it.value}" } + sharedSecret

        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(concatenated.toByteArray())
        val bigInt = BigInteger(1, digest)

        return bigInt.toString(16).padStart(32, '0')
    }
}
