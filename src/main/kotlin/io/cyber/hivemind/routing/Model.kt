package io.cyber.hivemind.routing

import io.cyber.hivemind.*
import io.cyber.hivemind.model.ResourceType
import io.cyber.hivemind.model.TrainModelReq
import io.cyber.hivemind.model.toUUID
import io.cyber.hivemind.service.FileService
import io.cyber.hivemind.service.MLService
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.content.LocalFileContent
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route

/**
 * Register [Model] routes.
 */
@KtorExperimentalLocationsAPI
fun Route.trainModel(fileService: FileService, mlService: MLService) {

    post<Model> {
        val trainModelReq = call.receive<TrainModelReq>()
        val trainRes = mlService.train(trainModelReq.scriptId.toUUID(),
                it.modelId.toUUID(), trainModelReq.dataId.toUUID())
        call.respond(trainRes)
    }

    get<Model> {
        val type = ContentType.Application.Zip
        val file = fileService.getZip(ResourceType.MODEL, it.modelId.toUUID(), true)
        call.respond(LocalFileContent(file, contentType = type))
    }
}
