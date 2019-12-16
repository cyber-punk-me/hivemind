package io.cyber.hivemind

import io.cyber.hivemind.routing.uploadData
import io.cyber.hivemind.service.DiskFileServiceImpl
import io.cyber.hivemind.service.FileService
import io.ktor.application.*
import io.ktor.features.Compression
import io.ktor.routing.*
import io.ktor.locations.*
import io.cyber.hivemind.routing.uploadScript


/**
 * Script upload.
 */
@Location("/script/{scriptId}")
class Script(val scriptId: String)

/**
 * Data upload.
 */
@Location("/data/{dataId}")
class Data(val dataId: String)


@KtorExperimentalLocationsAPI
fun Application.main() {

    // Allows to use classes annotated with @Location to represent URLs.
    // They are typed, can be constructed to generate URLs, and can be used to register routes.
    install(Locations)
    install(Compression)

    val fileService: FileService = DiskFileServiceImpl()

    routing {
        uploadScript(fileService)
        uploadData(fileService)
    }
}