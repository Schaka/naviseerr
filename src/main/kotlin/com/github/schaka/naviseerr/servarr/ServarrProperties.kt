package com.github.schaka.naviseerr.servarr

interface ServarrProperties : RestClientProperties {
    val determineAgeBy: HistorySort?
}