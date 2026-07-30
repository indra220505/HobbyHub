package com.hobbyhub

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class HobbyHubApplication

fun main(args: Array<String>) {
    runApplication<HobbyHubApplication>(*args)
}
