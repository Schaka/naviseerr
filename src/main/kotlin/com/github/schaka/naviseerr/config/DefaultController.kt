package com.github.schaka.naviseerr.config

import org.springframework.core.Ordered.LOWEST_PRECEDENCE
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Order(LOWEST_PRECEDENCE)
@Controller
class DefaultController {

    @Order(LOWEST_PRECEDENCE)
    @GetMapping("{_:^(?!index\\.html|api|error).*$}")
    fun fallback(): String {
        return "forward:/index.html"
    }

}