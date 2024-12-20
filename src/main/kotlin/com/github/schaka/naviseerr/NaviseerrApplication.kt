package com.github.schaka.naviseerr

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableConfigurationProperties
@EnableAsync
@EnableCaching
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
class NaviseerrApplication

fun main(args: Array<String>) {
    runApplication<NaviseerrApplication>(*args)
}

