package io.cyber.hivemind

import io.cyber.hivemind.service.DiskFileServiceImpl
import io.cyber.hivemind.service.FileService
import io.ktor.application.*
import io.ktor.features.Compression
import io.ktor.routing.*
import io.ktor.locations.*
import io.ktor.samples.youkube.uploadScript


/**
 * Script upload.
 */
@Location("/script/{scriptId}")
class Upload


fun Application.main() {

    // Allows to use classes annotated with @Location to represent URLs.
    // They are typed, can be constructed to generate URLs, and can be used to register routes.
    install(Locations)
    install(Compression)

    val fileService: FileService = DiskFileServiceImpl()

    routing {
        uploadScript(database, uploadDir)
    }
}