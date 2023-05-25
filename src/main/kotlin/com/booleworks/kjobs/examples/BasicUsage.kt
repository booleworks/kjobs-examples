package com.booleworks.kjobs.examples

import com.booleworks.kjobs.api.JobFramework
import com.booleworks.kjobs.api.persistence.hashmap.HashMapDataPersistence
import com.booleworks.kjobs.api.persistence.hashmap.HashMapJobPersistence
import com.booleworks.kjobs.control.ComputationResult
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

// Main method to set up the Ktor Webservice on localhost:8080
fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    // Setup persistence (this is the simplest kind of persistence which should only be used for testing purposes!)
    val jobPersistence = HashMapJobPersistence()
    val dataPersistence = HashMapDataPersistence<String, String>(jobPersistence)

    // initialize Webservice routing
    routing {
        // create a basic route 'optimization'
        route("computation") {
            // initialize the KJobs JobFramework
            JobFramework(myInstanceName = "ME", jobPersistence, application = this@module) {
                // add an API
                addApi(
                    // name of the job type, only used internally to distinguish different APIs
                    jobType = "My-first-KJobs-API",
                    // the route on which the API should be generated
                    route = this@route,
                    // the persistence where jobs are store (together with their inputs and results)
                    dataPersistence,
                    // how the service should accept incoming requests to the 'submit' method (will usually be some object via JSON)
                    inputReceiver = { call.receiveText() },
                    // how the service should return the result (will usually be some object via JSON)
                    resultResponder = { call.respondText(it) },
                    // how the actual computation is performed
                    { _, input -> ComputationResult.Success<String>("Result of $input") })
            }
        }
    }
}
