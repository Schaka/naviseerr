package com.github.schaka.naviseerr.library

import com.github.schaka.naviseerr.db.library.MediaRequestService
import com.github.schaka.naviseerr.db.user.NaviseerrUser
import com.github.schaka.naviseerr.library.dto.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/requests")
class RequestController(
    private val requestService: RequestService,
    private val mediaRequestService: MediaRequestService,
) {

    @PostMapping("/artist")
    fun requestArtist(
        @AuthenticationPrincipal principal: NaviseerrUser,
        @RequestBody request: ArtistRequestDto
    ): ResponseEntity<MediaRequestDto> {
        return try {
            val result = requestService.requestArtist(principal, request.musicbrainzId, request.name)
            ResponseEntity.ok(result.toDto())
        } catch (e: MediaAlreadyAvailableException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    @PostMapping("/album")
    fun requestAlbum(
        @AuthenticationPrincipal principal: NaviseerrUser,
        @RequestBody request: AlbumRequestDto
    ): ResponseEntity<MediaRequestDto> {
        return try {
            val result = requestService.requestAlbum(
                principal,
                request.musicbrainzArtistId,
                request.musicbrainzAlbumId,
                request.artistName,
                request.albumTitle
            )
            ResponseEntity.ok(result.toDto())
        } catch (e: MediaAlreadyAvailableException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    @GetMapping
    fun getMyRequests(@AuthenticationPrincipal principal: NaviseerrUser): ResponseEntity<List<MediaRequestDto>> {
        return ResponseEntity.ok(mediaRequestService.findByUser(principal.id).map { it.toDto() })
    }

    @GetMapping("/all")
    fun getAllRequests(): ResponseEntity<List<MediaRequestDto>> {
        return ResponseEntity.ok(mediaRequestService.findAll().map { it.toDto() })
    }
}
