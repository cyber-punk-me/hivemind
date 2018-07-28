package io.cyber.hivemind.util

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.ext.web.RoutingContext
import java.io.UnsupportedEncodingException
import java.net.URLDecoder



/**
 * User: kirillskiy
 * Date: 22/07/2018
 * Time: 21:26
 */
fun uploadFile(routingContext : RoutingContext, vertx : Vertx) : Buffer? {
    val fileUploadSet = routingContext.fileUploads()
    val fileUploadIterator = fileUploadSet.iterator()
    while (fileUploadIterator.hasNext()) {
        val fileUpload = fileUploadIterator.next()

        // To get the uploaded file do
        val uploadedFile = vertx.fileSystem().readFileBlocking(fileUpload.uploadedFileName())

        // Uploaded File Name
        try {
            val fileName = URLDecoder.decode(fileUpload.fileName(), "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        return uploadedFile
    }
    return null
}

fun downloadFile(routingContext: RoutingContext, fileName : String) {
    routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/zip")
          .putHeader("Content-Disposition", "attachment; filename=\"$fileName\"")
          .putHeader(HttpHeaders.TRANSFER_ENCODING, "chunked")
          .sendFile(fileName)
}