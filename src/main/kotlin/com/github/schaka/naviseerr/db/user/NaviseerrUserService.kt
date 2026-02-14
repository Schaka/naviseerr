package com.github.schaka.naviseerr.db.user

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class NaviseerrUserService {

    fun storeUserLogin(username: String, navidromeId: String, token: String, subsonicToken: String, subsonicSalt: String): NaviseerrUser {
        return transaction {
            val existing = NaviseerrUsers
                .selectAll().where { NaviseerrUsers.username eq username }
                .singleOrNull()

            if (existing != null) {
                NaviseerrUsers.update({ NaviseerrUsers.username eq username }) {
                    it[lastLogin] = Instant.now()
                    it[this.navidromeToken] = token
                    it[this.subsonicToken] = subsonicToken
                    it[this.subsonicSalt] = subsonicSalt
                }
                mapRow(existing).copy(navidromeToken = token, subsonicToken = subsonicToken, subsonicSalt = subsonicSalt, lastLogin = Instant.now())
            } else {
                val id = UUID.randomUUID()
                NaviseerrUsers.insert {
                    it[this.id] = id
                    it[this.username] = username
                    it[this.navidromeId] = navidromeId
                    it[this.navidromeToken] = token
                    it[this.subsonicToken] = subsonicToken
                    it[this.subsonicSalt] = subsonicSalt
                    it[lastLogin] = Instant.now()
                }
                NaviseerrUser(id, username, navidromeId, token, subsonicSalt, subsonicToken, Instant.now())
            }
        }
    }

    fun getAllUsers(): List<NaviseerrUser> {
        return transaction {
            NaviseerrUsers.selectAll().map(::mapRow)
        }
    }

    fun getUserByUsername(username: String): NaviseerrUser? {
        return transaction {
            NaviseerrUsers
                .selectAll().where { NaviseerrUsers.username eq username }
                .singleOrNull()
                ?.let(::mapRow)
        }
    }

    fun getUsersWithApiKeys(): List<NaviseerrUser> {
        return transaction {
            NaviseerrUsers
                .selectAll()
                .where {
                    (NaviseerrUsers.lastFMSessionKey neq null) or (NaviseerrUsers.listenBrainzApiKey neq null)
                }
                .map(::mapRow)
        }
    }

    fun updateLastFMSessionKey(username: String, sessionKey: String) {
        transaction {
            NaviseerrUsers.update({ NaviseerrUsers.username eq username }) {
                it[lastFMSessionKey] = sessionKey
            }
        }
    }

    fun updateListenBrainzToken(username: String, token: String?) {
        transaction {
            NaviseerrUsers.update({ NaviseerrUsers.username eq username }) {
                it[listenBrainzApiKey] = token
            }
        }
    }

    fun updateScrobblingPreferences(
        username: String,
        lastFmEnabled: Boolean? = null,
        listenBrainzEnabled: Boolean? = null
    ) {
        transaction {
            NaviseerrUsers.update({ NaviseerrUsers.username eq username }) {
                if (lastFmEnabled != null) {
                    it[lastFmScrobblingEnabled] = lastFmEnabled
                }
                if (listenBrainzEnabled != null) {
                    it[listenBrainzScrobblingEnabled] = listenBrainzEnabled
                }
            }
        }
    }

    fun getUserById(userId: UUID): NaviseerrUser? {
        return transaction {
            NaviseerrUsers
                .selectAll().where { NaviseerrUsers.id eq userId }
                .singleOrNull()
                ?.let(::mapRow)
        }
    }

    private fun mapRow(row: ResultRow) =
        NaviseerrUser(
            id = row[NaviseerrUsers.id].value,
            username = row[NaviseerrUsers.username],
            navidromeId = row[NaviseerrUsers.navidromeId],
            navidromeToken = row[NaviseerrUsers.navidromeToken],
            subsonicToken = row[NaviseerrUsers.subsonicToken],
            subsonicSalt = row[NaviseerrUsers.subsonicSalt],
            lastLogin = row[NaviseerrUsers.lastLogin],
            lastFmSessionKey = row[NaviseerrUsers.lastFMSessionKey],
            listenBrainzToken = row[NaviseerrUsers.listenBrainzApiKey],
            lastFmScrobblingEnabled = row[NaviseerrUsers.lastFmScrobblingEnabled],
            listenBrainzScrobblingEnabled = row[NaviseerrUsers.listenBrainzScrobblingEnabled]
        )
}