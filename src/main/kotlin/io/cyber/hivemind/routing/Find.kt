package io.cyber.hivemind.routing

import io.cyber.hivemind.*
import io.cyber.hivemind.service.MLService
import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.post
import io.ktor.response.respond
import io.ktor.routing.Route

/**
 * Register [ModelFind] routes.
 */
@KtorExperimentalLocationsAPI
fun Route.findModel(mlService: MLService) {
    post<ModelFind> {
        val result = mlService.getModelsInServing(false) + mlService.getModelsInTraining(false)
        call.respond(result)
    }
}

//todo find stopped models, data, scripts