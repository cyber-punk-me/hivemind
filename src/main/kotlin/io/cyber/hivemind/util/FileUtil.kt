package io.cyber.hivemind.util

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

fun getNextFileName(files : List<String>?, extension: String?) : String {
    val extRes = extension?.let { ".$extension" } ?: ""
    val defaultName = "0$extRes"
    if (files == null || files.isEmpty()) return defaultName
    val maxFile : Int? = files.map { f -> f.split(File.separator).last() }.map{s -> s.cutEtension()}. filter { f -> isInteger(f) }.map { s -> Integer.parseInt(s) }.max()
    return when {
        maxFile != null -> "" + (maxFile + 1) + extRes
        else -> defaultName
    }
}

private fun String.cutEtension(): String {
    if (!contains(".")) return this
    return split(".")[0]
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
        var firstEntryRead = true
        var rootEntryDir : String? = null
        ZipInputStream(BufferedInputStream(`is`)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val entryName = entry.name
                if (!entryName.isEmpty()) {
                    if (entry.isDirectory) {
                        if (firstEntryRead) {
                            rootEntryDir = entry.name
                        }
                        val resDirName = if (rootEntryDir == null) entryName else entryName.substring(rootEntryDir!!.length)
                        if (!resDirName.isEmpty() && !File(directory, resDirName).mkdir()) {
                            print("Failed to create directory")
                            return
                        }
                    } else {
                        val buff = ByteArray(bufferSize)
                        var dest: BufferedOutputStream? = null
                        try {
                            val resFileNme = if (rootEntryDir == null) entryName else entryName.substring(rootEntryDir!!.length)
                            val fos = FileOutputStream(File(directory, resFileNme))
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
                firstEntryRead = false
            }
        }
    }
}
