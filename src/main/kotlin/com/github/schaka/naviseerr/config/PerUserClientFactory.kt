package com.github.schaka.naviseerr.config

import com.github.schaka.naviseerr.db.user.NaviseerrUser

/**
 * Common interface for factories that manage per-user HTTP clients.
 * Implementations should cache clients and invalidate/replace them when user tokens change.
 */
interface PerUserClientFactory<T> {

    /**
     * Gets an existing client for the user or creates a new one.
     * If the user's token has changed, the cached client is replaced.
     */
    fun getOrCreateClient(user: NaviseerrUser): T

    /**
     * Clears the cached client for a specific user.
     * Useful when forcing a refresh or handling logout.
     */
    fun invalidateClient(userId: java.util.UUID)

    /**
     * Clears all cached clients.
     */
    fun invalidateAll()
}
