package com.booleworks.kjobs.examples.additionalroutes

import com.booleworks.kjobs.ExampleConfig
import com.booleworks.kjobs.api.JobFramework
import com.booleworks.kjobs.api.persistence.hashmap.HashMapDataPersistence
import com.booleworks.kjobs.api.persistence.hashmap.HashMapJobPersistence
import com.booleworks.kjobs.control.ComputationResult
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * This example assumes that you are already familiar with the [Basic Usage][com.booleworks.kjobs.examples.basicusage.main] example.
 *
 * In this example we will show routes which can be created by KJobs for each API. In the Basic Usage example you have seen the routes
 * - `POST submit` for submitting a job
 * - `GET status/{uuid}` for checking the status of a job
 * - `GET result/{uuid}` for retrieving the result of a job
 *
 * Additionally, KJobs **always** creates the resource
 * - `GET failure/{uuid}` which returns a plain text string with an error message, if the status of the job is `FAILURE`
 *
 * Furthermore, there are some more resources which can optionally be configured (all of them are disabled by default):
 * - `POST synchronous` is a synchronous wrapper for the asynchronous API
 * - `DELETE delete/{uuid}` deletes a job and its input and output (but the job must be finished, i.e. be in status `SUCCESS`,
 *   `FAILURE` or `CANCELLED`)
 * - `POST cancel/{uuid}` cancels the job if it is in status `CREATED` or `RUNNING`. If it is in status `CREATED` it will directly
 *   go to status `CANCELLED` and will not be computed. If it is `RUNNING` it will go to `CANCEL_REQUESTED` and wait for the job to
 *   terminate (it depends on the job implementation how quickly the job will actually termine). Once terminated, the job will go to
 *   status `CANCELLED`.
 * - `GET info/{uuid}` returns arbitrary information about the job
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
                    { _, input ->
                        // The computation will expect an integer value N as input. If it is not an integer, the computation
                        // will abort with an error. Otherwise, it will wait (delay) for N milliseconds and then return a result.
                        // This allows us in the examples to provoke an error or simulate a long running computation.
                        val delayMs = input.toIntOrNull()
                        if (delayMs == null) {
                            ComputationResult.Error("Input should be an integer", tryRepeat = false)
                        } else {
                            delay(delayMs.milliseconds)
                            ComputationResult.Success<String>("Result of '$input'")
                        }
                    }
                ) {
                    // Configuration of the synchronous resource
                    synchronousResourceConfig {
                        enabled = true
                        // The synchronous resource is a wrapper around the asynchronous API, so it internally calls submit and then
                        // has to wait until the job finishes. The `checkInterval` defines the interval in which the status of the job
                        // is checked
                        checkInterval = 200.milliseconds
                        // If the maximum waiting time is exceeded, the request will abort with HTTP code 400 and a respective message.
                        // Note that for large values you will also have to adjust the default connection timeouts of client and server.
                        maxWaitingTime = 10000.milliseconds
                    }
                    // Configuration of the info resource
                    infoConfig {
                        enabled = true
                        // The responder receives the job and defines what should be responded by the application call.
                        responder = { job -> call.respond("This job was submitted at ${job.createdAt} and is currently in status ${job.status}.") }
                    }
                    // Enable deletion of finished jobs
                    apiConfig {
                        enableDeletion = true
                    }
                }
            }
        }
        // Cancellation is currently only configurable globally
        cancellationConfig {
            enabled = true
            // each instance (pod) needs to check regularly for newly cancelled jobs by querying the persistence,
            // this is the interval in which these checks happen
            checkInterval = 500.milliseconds
        }
    }
}
