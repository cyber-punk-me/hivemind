package io.cyber.hivemind.routing

import com.fasterxml.jackson.databind.node.ObjectNode
import io.cyber.hivemind.Apply
import io.cyber.hivemind.service.MLService
import io.cyber.hivemind.model.toUUID
import io.cyber.hivemind.util.fromJson
import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.post
import io.ktor.request.receiveStream
import io.ktor.response.respond
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.applyData(mlService: MLService) {

    post<Apply> {
        val inputData = call.receiveStream()
        val json = fromJson(inputData, ObjectNode::class.java)
        val result = mlService.applyData(it.modelId.toUUID(), json)
        call.respond(result)
    }
}