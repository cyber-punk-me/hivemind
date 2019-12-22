package io.cyber.hivemind

import io.cyber.hivemind.routing.applyData
import io.cyber.hivemind.routing.trainModel
import io.cyber.hivemind.routing.uploadData
import io.cyber.hivemind.service.DiskFileServiceImpl
import io.cyber.hivemind.service.FileService
import io.ktor.application.*
import io.ktor.features.Compression
import io.ktor.routing.*
import io.ktor.locations.*
import io.cyber.hivemind.routing.uploadScript
import io.cyber.hivemind.service.MLService
import io.cyber.hivemind.service.MLServiceImpl
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter


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

/**
 * Model train.
 */
@Location("/model/{modelId}")
class Model(val modelId: String)

/**
 * Model train.
 */
@Location("/apply/{modelId}")
class Apply(val modelId: String)


@KtorExperimentalLocationsAPI
fun Application.main() {

    // Allows to use classes annotated with @Location to represent URLs.
    // They are typed, can be constructed to generate URLs, and can be used to register routes.
    install(Locations)
    install(Compression)
    install(CallLogging) {
        level = org.slf4j.event.Level.INFO
    }
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter())
    }
    val fileService: FileService = DiskFileServiceImpl()
    val mlService: MLService = MLServiceImpl(fileService)

    routing {
        uploadScript(fileService)
        uploadData(fileService)
        trainModel(fileService, mlService)
        applyData(mlService)
    }
}