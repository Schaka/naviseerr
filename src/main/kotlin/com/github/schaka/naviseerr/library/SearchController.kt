package com.github.schaka.naviseerr.library

import com.github.schaka.naviseerr.db.library.LibraryAlbumService
import com.github.schaka.naviseerr.db.library.LibraryArtistService
import com.github.schaka.naviseerr.db.user.NaviseerrUser
import com.github.schaka.naviseerr.library.dto.AlbumSearchResultDto
import com.github.schaka.naviseerr.library.dto.ArtistSearchResultDto
import com.github.schaka.naviseerr.library.dto.SearchResultDto
import com.github.schaka.naviseerr.musicbrainz.MusicBrainzService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/search")
class SearchController(
    private val musicBrainzService: MusicBrainzService,
    private val libraryArtistService: LibraryArtistService,
    private val libraryAlbumService: LibraryAlbumService,
) {

    @GetMapping("/artists")
    fun searchArtists(
        @AuthenticationPrincipal principal: NaviseerrUser,
        @RequestParam query: String
    ): ResponseEntity<SearchResultDto<ArtistSearchResultDto>> {
        val mbResults = musicBrainzService.searchArtists(principal.username, query)
        val results = mbResults.artists.map { artist ->
            val inLibrary = libraryArtistService.findByMusicbrainzId(artist.id)
            ArtistSearchResultDto(
                musicbrainzId = artist.id,
                name = artist.name,
                disambiguation = artist.disambiguation,
                type = artist.type,
                country = artist.country,
                status = inLibrary?.status?.name
            )
        }
        return ResponseEntity.ok(SearchResultDto(results, mbResults.count))
    }

    @GetMapping("/albums")
    fun searchAlbums(
        @AuthenticationPrincipal principal: NaviseerrUser,
        @RequestParam query: String
    ): ResponseEntity<SearchResultDto<AlbumSearchResultDto>> {
        val mbResults = musicBrainzService.searchReleaseGroups(principal.username, query)
        val results = mbResults.releaseGroups.map { rg ->
            val inLibrary = libraryAlbumService.findByMusicbrainzId(rg.id)
            val artistCredit = rg.artistCredit?.firstOrNull()
            AlbumSearchResultDto(
                musicbrainzId = rg.id,
                title = rg.title,
                primaryType = rg.primaryType,
                firstReleaseDate = rg.firstReleaseDate,
                artistName = artistCredit?.name ?: "Unknown Artist",
                artistMusicbrainzId = artistCredit?.artist?.id ?: "",
                status = inLibrary?.status?.name
            )
        }
        return ResponseEntity.ok(SearchResultDto(results, mbResults.count))
    }
}
