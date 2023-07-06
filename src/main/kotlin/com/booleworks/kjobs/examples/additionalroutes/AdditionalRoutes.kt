package com.booleworks.kjobs.examples.additionalroutes

import com.booleworks.kjobs.ExampleConfig
import com.booleworks.kjobs.api.JobFramework
import com.booleworks.kjobs.api.persistence.hashmap.HashMapDataPersistence
import com.booleworks.kjobs.api.persistence.hashmap.HashMapJobPersistence
import com.booleworks.kjobs.control.ComputationResult
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.request.path
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.milliseconds

/**
 * This example assumes that you are already familiar with the [Basic Usage][com.booleworks.kjobs.examples.basicusage.main] example.
 *
 * In this example we will show routes which can be created by KJobs for each API, additionally to the default routes `submit`,
 * `status`, and `result`.
 */
fun main() {
    embeddedServer(CIO, port = ExampleConfig.PORT, host = ExampleConfig.HOST, module = Application::module).start(wait = true)
}

fun Application.module() {
    // Setup persistence (this is the simplest kind of persistence which should only be used for testing purposes!)
    val jobPersistence = HashMapJobPersistence()
    val simpleComputationPersistence = HashMapDataPersistence<String, String>(jobPersistence)

    JobFramework(myInstanceName = "ME", jobPersistence) {
        routing {
            route("additional-routes-example") {
                addApi(
                    jobType = "AdditionalRoutes",
                    route = this@route,
                    simpleComputationPersistence,
                    inputReceiver = { call.receiveText() },
                    resultResponder = { call.respondText(it) },
                    // TODO: explain
                    { _, input ->
                        val delayMs = input.toIntOrNull()
                        if (delayMs == null) {
                            ComputationResult.Error("Input should be an integer", tryRepeat = false)
                        } else {
                            delay(delayMs.milliseconds)
                            ComputationResult.Success<String>("Result of '$input'")
                        }
                    }
                ) {
                    // TODO: explain each option
                    synchronousResourceConfig {
                        enabled = true
                        checkInterval = 200.milliseconds
                        maxWaitingTime = 10000.milliseconds
                    }
                    infoConfig {
                        enabled = true
                        responder = { call.respond("This job was submitted at ${it.createdAt} and is currently in status ${it.status}.") }
                    }
                    apiConfig {
                        enableDeletion = true
                    }
                }
            }
        }
        // TODO: explain each option
        cancellationConfig {
            enabled = true
            checkInterval = 500.milliseconds
        }
    }
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
}
