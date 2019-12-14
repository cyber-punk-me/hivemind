package io.ktor.samples.youkube

import io.cyber.hivemind.Upload
import io.cyber.hivemind.service.FileService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.network.util.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import kotlinx.coroutines.*
import java.io.*

/**
 * Register [Upload] routes.
 */
fun Route.uploadScript(fileService: FileService) {

    post<Upload> {
        val multipart = call.receiveMultipart()
        var title = ""
        var videoFile: File? = null

        // Processes each part of the multipart input content of the user
        multipart.forEachPart { part ->
            if (part is PartData.FormItem) {
                if (part.name == "title") {
                    title = part.value
                }
            } else if (part is PartData.FileItem) {
                val ext = File(part.originalFileName).extension
                val file = File(
                        uploadDir,
                        "upload-${System.currentTimeMillis()}-${session.userId.hashCode()}-${title.hashCode()}.$ext"
                )

                part.streamProvider().use { its -> file.outputStream().buffered().use { its.copyToSuspend(it) } }
                videoFile = file
            }

            part.dispose()
        }

        val id = database.addVideo(title, session.userId, videoFile!!)

    }
}

/**
 * Utility boilerplate method that suspending,
 * copies a [this] [InputStream] into an [out] [OutputStream] in a separate thread.
 *
 * [bufferSize] and [yieldSize] allows to control how and when the suspending is performed.
 * The [dispatcher] allows to specify where will be this executed (for example a specific thread pool).
 */
suspend fun InputStream.copyToSuspend(
        out: OutputStream,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        yieldSize: Int = 4 * 1024 * 1024,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
): Long {
    return withContext(dispatcher) {
        val buffer = ByteArray(bufferSize)
        var bytesCopied = 0L
        var bytesAfterYield = 0L
        while (true) {
            val bytes = read(buffer).takeIf { it >= 0 } ?: break
            out.write(buffer, 0, bytes)
            if (bytesAfterYield >= yieldSize) {
                yield()
                bytesAfterYield %= yieldSize
            }
            bytesCopied += bytes
            bytesAfterYield += bytes
        }
        return@withContext bytesCopied
    }
}
