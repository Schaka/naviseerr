package com.github.schaka.naviseerr.servarr

import com.github.schaka.naviseerr.servarr.HistorySort.MOST_RECENT
import com.github.schaka.naviseerr.servarr.history.HistoryResponse
import java.time.LocalDateTime

interface ServarrService {

    fun getEntries(): List<LibraryItem>

    fun removeEntries(items: List<LibraryItem>)

    /**
     * Sort by oldest file. If upgrades are allowed, sort by most recently grabbed files.
     */
    fun byDate(sort: HistorySort): Comparator<LibraryItem> {
        val comp = compareBy<LibraryItem> { it.importedDate }
        return if (sort == MOST_RECENT) comp.reversed() else comp
    }

    fun byHistoryMostRecent(): Comparator<HistoryResponse> {
        val comp = compareBy<HistoryResponse> { LocalDateTime.parse(it.date.substring(0, it.date.length - 1)) }
        return comp.reversed()
    }
}