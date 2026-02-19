package com.github.schaka.naviseerr

import org.springframework.boot.SpringApplication

fun main(args: Array<String>) {
    LocalContainers // runs LocalContainers init method!
    SpringApplication.run(NaviseerrApplication::class.java, *args)
}
