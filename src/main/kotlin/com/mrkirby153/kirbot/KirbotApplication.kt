package com.mrkirby153.kirbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KirbotApplication

fun main(args: Array<String>) {
    runApplication<KirbotApplication>(*args)
}
