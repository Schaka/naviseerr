package com.github.schaka.naviseerr.db.activity

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Service

@Service
class UserPlayService {

    /**
     * Creates a database entry for this play. If already existing, returns play with correct database id.
     */
    fun createPlay(play: UserPlay): UserPlay = transaction {
        val existingPlay = UserPlays.selectAll()
            .where { UserPlays.navidromePlayId eq play.navidromePlayId }
            .singleOrNull()

        if (existingPlay != null) {
            return@transaction mapRowToUserPlay(existingPlay)
        }

        UserPlays.insert {
            it[id] = play.id
            it[userId] = play.userId
            it[navidromePlayId] = play.navidromePlayId
            it[trackId] = play.trackId
            it[trackName] = play.trackName
            it[artistName] = play.artistName
            it[albumName] = play.albumName
            it[duration] = play.duration
            it[playedAt] = play.playedAt
            it[musicBrainzTrackId] = play.musicBrainzTrackId
            it[musicBrainzArtistId] = play.musicBrainzArtistId
            it[musicBrainzAlbumId] = play.musicBrainzAlbumId
            it[createdAt] = play.createdAt
        }

        return@transaction play
    }


    private fun mapRowToUserPlay(row: ResultRow) = UserPlay(
        id = row[UserPlays.id].value,
        userId = row[UserPlays.userId].value,
        navidromePlayId = row[UserPlays.navidromePlayId],
        trackId = row[UserPlays.trackId],
        trackName = row[UserPlays.trackName],
        artistName = row[UserPlays.artistName],
        albumName = row[UserPlays.albumName],
        duration = row[UserPlays.duration],
        playedAt = row[UserPlays.playedAt],
        musicBrainzTrackId = row[UserPlays.musicBrainzTrackId],
        musicBrainzArtistId = row[UserPlays.musicBrainzArtistId],
        musicBrainzAlbumId = row[UserPlays.musicBrainzAlbumId],
        createdAt = row[UserPlays.createdAt]
    )
}
