package com.github.schaka.naviseerr.listenbrainz.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

/**
 * Request payload for submitting listens to ListenBrainz.
 * https://listenbrainz.readthedocs.io/en/production/dev/api/#post--1-submit-listens
 */
data class SubmitListensRequest(
    @JsonProperty("listen_type")
    val listenType: String = "single",

    @JsonProperty("payload")
    val payload: List<Listen>
)

/**
 * A single listen submission.
 */
data class Listen(
    @JsonProperty("listened_at")
    val listenedAt: Long,

    @JsonProperty("track_metadata")
    val trackMetadata: TrackMetadata
)

/**
 * Track metadata for a listen.
 */
data class TrackMetadata(
    @JsonProperty("artist_name")
    val artistName: String,

    @JsonProperty("track_name")
    val trackName: String,

    @JsonProperty("release_name")
    val releaseName: String? = null,

    @JsonProperty("additional_info")
    val additionalInfo: AdditionalInfo? = null
)

/**
 * Additional metadata for a track.
 */
data class AdditionalInfo(
    @JsonProperty("duration_ms")
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val durationMs: Int? = null
)
