package com.github.schaka.naviseerr.download_client.slskd

import com.github.schaka.naviseerr.download_client.slskd.dto.File
import com.github.schaka.naviseerr.download_client.slskd.dto.Search
import com.github.schaka.naviseerr.download_client.slskd.dto.SearchEntry
import com.github.schaka.naviseerr.download_client.slskd.dto.SearchResult
import kotlinx.coroutines.delay
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.stereotype.Service

@RegisterReflectionForBinding(classes = [Search::class, File::class, SearchResult::class, SearchEntry::class])
@Service
class SoulseekRestService(

    private val soulseekClient: SoulseekClient
) {

    suspend fun search(searchText: String): List<SearchResult> {
        val search = Search(searchText)
        soulseekClient.searchForMedia(search)
        waitForSearch(search.id)
        return soulseekClient.searchContent(search.id)
    }

    suspend fun waitForSearch(id: String) {
        while (soulseekClient.searchForMedia().filter { it.id == id }.firstOrNull()?.isComplete == false) {
            delay(1000)
        }
    }


}