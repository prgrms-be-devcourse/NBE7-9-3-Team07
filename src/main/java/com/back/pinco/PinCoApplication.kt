package com.back.pinco

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class PinCoApplication

fun main(args: Array<String>) {
    runApplication<PinCoApplication>(*args)
}

