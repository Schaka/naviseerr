package com.github.schaka.naviseerr.lastfm.dto

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement

@XmlAccessorType(XmlAccessType.FIELD)
data class LastFMSession(
    @field:XmlElement(name = "name")
    val name: String = "",

    @field:XmlElement(name = "key")
    val key: String = "",

    @field:XmlElement(name = "subscriber")
    val subscriber: Int = 0
)
