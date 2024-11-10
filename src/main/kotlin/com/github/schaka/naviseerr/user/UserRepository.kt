package com.github.schaka.naviseerr.user

import com.github.schaka.naviseerr.db.Tables.NAVIDROME_USERS
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class UserRepository(
    private val create: DSLContext
) {

    fun createNewUser(navidromeId: String) {
        create
            .insertInto(NAVIDROME_USERS, NAVIDROME_USERS.ID, NAVIDROME_USERS.LAST_FM_API_KEY, NAVIDROME_USERS.LAST_FM_USERNAME)
            .values(navidromeId, null, null)
            .onDuplicateKeyIgnore()
            .execute()
    }

    fun getUser(navidromeId: String): UserData {
        return create.select(NAVIDROME_USERS.ID, NAVIDROME_USERS.LAST_FM_API_KEY, NAVIDROME_USERS.LAST_FM_USERNAME)
            .from(NAVIDROME_USERS)
            .where(NAVIDROME_USERS.ID.eq(navidromeId))
            .fetchOne { UserData(it.value1(), it.value2(), it.value3()) } ?: throw IllegalStateException("User for id $navidromeId does not exist")
    }

    fun updateUser(user: UserData): UserData {
        create.update(NAVIDROME_USERS)
            .set(NAVIDROME_USERS.LAST_FM_API_KEY, user.lastFmKey)
            .set(NAVIDROME_USERS.LAST_FM_USERNAME, user.lastFmUsername)
            .where(NAVIDROME_USERS.ID.eq(user.navidromeId))
            .execute()

        return user
    }
}