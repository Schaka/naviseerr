package com.github.schaka.naviseerr.download_client.slskd

import com.github.schaka.naviseerr.download_client.slskd.dto.SearchFile
import com.github.schaka.naviseerr.download_client.slskd.dto.Search
import com.github.schaka.naviseerr.download_client.slskd.dto.SearchEntry
import com.github.schaka.naviseerr.download_client.slskd.dto.SearchResult
import kotlinx.coroutines.delay
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.stereotype.Service

@RegisterReflectionForBinding(classes = [Search::class, SearchFile::class, SearchResult::class, SearchEntry::class])
@Service
class SoulseekRestService(

    private val soulseekClient: SoulseekClient
) {

    suspend fun search(searchText: String): List<SearchResult> {
        val search = Search(searchText)
        soulseekClient.searchForMedia(search)
        waitForSearch(search.id)
        val searchResult = soulseekClient.searchContent(search.id)
        soulseekClient.deleteSearch(search.id)
        return searchResult
    }

    suspend fun waitForSearch(id: String) {
        //soulseekClient.listSearches().filter { it.id == id }.firstOrNull()?
        while (!soulseekClient.searches(id).isComplete) {
            delay(1000)
        }
    }


}