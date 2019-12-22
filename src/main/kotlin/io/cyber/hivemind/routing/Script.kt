package io.cyber.hivemind.routing

import io.cyber.hivemind.model.ResourceType
import io.cyber.hivemind.Script
import io.cyber.hivemind.service.FileService
import io.cyber.hivemind.model.toUUID
import io.ktor.application.*
import io.ktor.http.ContentType
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.respond
import io.ktor.routing.*

/**
 * Register [Script] routes.
 */
@KtorExperimentalLocationsAPI
fun Route.uploadScript(fileService: FileService) {

    post<Script> {
        val multipart = call.receiveMultipart()
        // Processes each part of the multipart input content of the user
        multipart.forEachPart { part ->
            if (part is PartData.FileItem) {
                fileService.storeScript(it.scriptId.toUUID(), part.streamProvider())
            }
            part.dispose()
        }
    }

    get<Script> {
        val type = ContentType.Application.Zip
        val file = fileService.getZip(ResourceType.SCRIPT, it.scriptId.toUUID())
        call.respond(LocalFileContent(file, contentType = type))
    }
}
