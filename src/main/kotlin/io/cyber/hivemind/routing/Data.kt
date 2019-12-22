package io.cyber.hivemind.routing


import io.cyber.hivemind.Data
import io.cyber.hivemind.model.ResourceType
import io.cyber.hivemind.constant.EXT
import io.cyber.hivemind.service.FileService
import io.cyber.hivemind.model.toUUID
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.LocalFileContent
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receiveStream
import io.ktor.response.respond
import io.ktor.routing.Route

/**
 * Register [Data] routes.
 */
@KtorExperimentalLocationsAPI
fun Route.uploadData(fileService: FileService) {

    post<Data> {
        //todo validate ext
        val binary = call.receiveStream()
        val extensionParam = call.parameters[EXT]
        val dataMeta = fileService.storeData(it.dataId.toUUID(), binary, extensionParam ?: "")
        call.respond(dataMeta)
    }

    get<Data> {
        val type = ContentType.Application.Zip
        val file = fileService.getZip(ResourceType.DATA, it.dataId.toUUID(), true)
        call.respond(LocalFileContent(file, contentType = type))
    }
}
