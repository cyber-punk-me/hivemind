package io.cyber.hivemind.util

import io.cyber.hivemind.service.MLServiceImpl
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.ext.web.RoutingContext
import java.io.*
import java.net.URLDecoder
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


/**
 * User: kirillskiy
 * Date: 22/07/2018
 * Time: 21:26
 */

val bufferSize = 4096

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

fun getNextFileName(files : List<String>?) : String {
    if (files == null || files.isEmpty()) return "0"
    val maxFile : Int? = files.map { f -> f.split(File.separator).last() }. filter { f -> isInteger(f) }.map { s -> Integer.parseInt(s) }.max()
    return when {
        maxFile != null -> "" + (maxFile + 1)
        else -> "0"
    }
}

fun isInteger(s: String): Boolean {
    try {
        Integer.parseInt(s)
    } catch (e: Exception) {
        return false
    }
    return true
}

fun unzipData(directory: File, zipFileName: String) {
    BufferedInputStream(FileInputStream(zipFileName)).use { `is` ->
        ZipInputStream(BufferedInputStream(`is`)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val entryName = entry.name
                if (!entryName.isEmpty()) {
                    if (entry.isDirectory) {
                        if (!File(directory, entryName).mkdir()) {
                            print("Failed to create directory")
                            return
                        }
                    } else {
                        val buff = ByteArray(bufferSize)
                        var dest: BufferedOutputStream? = null
                        try {
                            val fos = FileOutputStream(File(directory, entryName))
                            dest = BufferedOutputStream(fos, bufferSize)
                            var count = zis.read(buff, 0, bufferSize)
                            while (count != -1) {
                                dest.write(buff, 0, count)
                                count = zis.read(buff, 0, bufferSize)
                            }
                            dest.flush()
                        } finally {
                            dest?.close()
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }
    }
}
