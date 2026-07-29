package com.hobbyhub

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HobbyHubApplication

fun main(args: Array<String>) {
    runApplication<HobbyHubApplication>(*args)
}
