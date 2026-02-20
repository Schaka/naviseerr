package com.github.schaka.naviseerr

import org.springframework.boot.SpringApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener

fun main(args: Array<String>) {
    val env = LocalDevEnvironment()
    env.start()

    val app = SpringApplication(NaviseerrApplication::class.java)
    app.addListeners(ApplicationListener<ApplicationReadyEvent> { env.logStartupInfo() })
    app.run(*args)
}
