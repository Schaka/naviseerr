package com.github.schaka.naviseerr.config

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping


@Order(Ordered.LOWEST_PRECEDENCE)
@Controller
class DefaultController {

    @Order(Ordered.LOWEST_PRECEDENCE - 1)
    @GetMapping(value = ["/{path:^(?!api|error|h2-console|index\\.html|.*\\.css|.*\\.js).*$}/{path2:^(?!.*\\.css|.*\\.js|.*\\.png|.*\\.jpg|.*\\.jpeg|.*\\.svg|.*\\.gif|.*\\.woff|.*\\.woff2|.*\\.tff|.*\\.otf|.*\\.ico).*$}/**"])
    fun fallback2(): String {
        return "forward:/index.html"
    }

    @Order(Ordered.LOWEST_PRECEDENCE)
    @GetMapping(value = ["/{path:^(?!api|error|h2-console|index\\.html|.*\\.css|.*\\.js).*$}"])
    fun fallback(): String {
        return "forward:/index.html"
    }
}