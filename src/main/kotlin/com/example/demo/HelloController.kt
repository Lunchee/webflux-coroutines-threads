package com.example.demo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.time.delay
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.APPLICATION_NDJSON_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Duration

@RestController
class HelloController(private val webClient: WebClient) {

    @GetMapping("/hello-suspend", produces = [APPLICATION_JSON_VALUE])
    suspend fun sayHelloSuspend(@RequestParam name: String): GreetingDto {

        // It executes within a blocking thread pool of Spring WebFlux with the Unconfined Dispatcher
        // because a controller method with a plain DTO return type is treated as blocking method by WebFlux.
        println("Running on ${Thread.currentThread()} at the beginning")

        executeWebClientRequest().awaitSingleOrNull()

        // No change so far.
        println("Running on ${Thread.currentThread()} after the WebClient request")

        // Switches to the DefaultExecutor after the delay because of the Unconfined Dispatcher.
        delay(Duration.ofSeconds(1))
        println("Running on ${Thread.currentThread()} after delay")

        return GreetingDto("Hi, $name")
    }

    @GetMapping("/hello-flow", produces = [APPLICATION_NDJSON_VALUE])
    fun sayHelloFlow(@RequestParam name: String): Flow<GreetingDto> = flow {

        // Executes on the non-blocking event-loop of WebFlux because of the Flow return type.
        println("Running on ${Thread.currentThread()} at the beginning")

        executeWebClientRequest().awaitSingleOrNull()

        // No change so far.
        println("Running on ${Thread.currentThread()} after the WebClient request")

        for (i in 1..3) {
            // Starts on the event loop and continues on the DefaultExecutor after the first delay.
            println("Running on ${Thread.currentThread()} before emit")
            emit(GreetingDto("$i: Hi, $name."))
            println("Running on ${Thread.currentThread()} after emit")

            // Switches to the DefaultExecutor after the delay because of the Unconfined Dispatcher.
            delay(Duration.ofMillis(100))
            println("Running on ${Thread.currentThread()} after delay")
        }

        // Switches to the DefaultExecutor after the delay because of the Unconfined Dispatcher.
        println("Running on ${Thread.currentThread()} after at the end")
    }

    @GetMapping("/hello-mono", produces = [APPLICATION_JSON_VALUE])
    fun sayHelloMono(@RequestParam name: String): Mono<GreetingDto> {

        // Executes on the non-blocking event-loop of WebFlux because of the Mono return type.
        println("Running on ${Thread.currentThread()} at the beginning")

        return executeWebClientRequest()
            .then(Mono.just(GreetingDto("Hi, $name.")))
            .doOnEach {
                // Same non-blocking thread.
                println("Running on ${Thread.currentThread()} at the end")
            }
    }

    private fun executeWebClientRequest() = webClient.get()
        .uri("https://www.example.com")
        .retrieve()
        .toMono()
        .doOnNext {
            // Continues on the same thread.
            println("Running on ${Thread.currentThread()} inside WebClient request")
        }
}