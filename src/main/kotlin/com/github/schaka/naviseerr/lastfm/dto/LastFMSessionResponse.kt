package com.github.schaka.naviseerr.lastfm.dto

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement

/**
 * Response from Last.fm auth.getSession API call
 */
@XmlRootElement(name = "lfm")
@XmlAccessorType(XmlAccessType.FIELD)
data class LastFMSessionResponse(
    @field:XmlAttribute(name = "status")
    val status: String = "",

    @field:XmlElement(name = "session")
    val session: LastFMSession = LastFMSession()
)