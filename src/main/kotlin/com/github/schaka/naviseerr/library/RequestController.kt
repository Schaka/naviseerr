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
        val result = requestService.requestArtist(principal, request.musicbrainzId, request.name)
        return ResponseEntity.ok(result.toDto())
    }

    @PostMapping("/album")
    fun requestAlbum(
        @AuthenticationPrincipal principal: NaviseerrUser,
        @RequestBody request: AlbumRequestDto
    ): ResponseEntity<MediaRequestDto> {
        val result = requestService.requestAlbum(
            principal,
            request.musicbrainzArtistId,
            request.musicbrainzAlbumId,
            request.artistName,
            request.albumTitle
        )
        return ResponseEntity.ok(result.toDto())
    }

    @GetMapping
    fun getMyRequests(@AuthenticationPrincipal principal: NaviseerrUser): ResponseEntity<List<MediaRequestDto>> {
        return ResponseEntity.ok(mediaRequestService.findByUser(principal.id).map { it.toDto() })
    }

    @GetMapping("/all")
    fun getAllRequests(): ResponseEntity<List<MediaRequestDto>> {
        return ResponseEntity.ok(mediaRequestService.findAll().map { it.toDto() })
    }

    @ExceptionHandler
    fun handleException(ex: MediaAlreadyAvailableException): ResponseEntity<MediaRequestDto> {
        return ResponseEntity.status(HttpStatus.CONFLICT).build()
    }

    @ExceptionHandler
    fun handleException(ex: SearchCooldownException): ResponseEntity<MediaRequestDto> {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build()
    }
}
