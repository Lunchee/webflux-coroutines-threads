package com.example.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient

@SpringBootApplication
class WebFluxCoroutinesApplication {

    @Bean
    fun webClient(builder: WebClient.Builder): WebClient {
        return builder.build()
    }
}

fun main(args: Array<String>) {
    runApplication<WebFluxCoroutinesApplication>(*args)
}