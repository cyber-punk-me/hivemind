package io.cyber.hivemind.util

import io.cyber.hivemind.model.ResourceType
import io.cyber.hivemind.constant.*
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.cio.writeChannel
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.copyAndClose
import org.apache.commons.lang.SystemUtils
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


/**
 * User: kirillskiy
 * Date: 22/07/2018
 * Time: 21:26
 */

const val BUFFER_SIZE = 4096

fun getNextDataFileName(directory: File, extension: String = ""): String {
    val files = (directory.listFiles()?.asList() as List<File>).map { it.name }
    val defaultName = "0$extension"
    if (files.isEmpty()) return defaultName
    val maxFile: Int? = files.map { f -> f.split(File.separator).last() }.map { s -> s.cutExtension() }
            .filter { f -> isInteger(f) }.map { s -> Integer.parseInt(s) }.max()
    return when {
        maxFile != null -> "" + (maxFile + 1) + extension
        else -> defaultName
    }
}

private fun String.cutExtension(): String {
    if (!contains(".")) return this
    return split(".")[0]
}

fun String.dockerHostDir(): String {
    return if (!SystemUtils.IS_OS_WINDOWS) {
        this
    } else {
        "/" + replace(":\\", "\\").replace("\\", "/")
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

fun unzipData(destination: File, zipFile: File) {
    BufferedInputStream(FileInputStream(zipFile)).use { `is` ->
        var firstEntryRead = true
        var rootEntryDir: String? = null
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
                        if (!resDirName.isEmpty() && !File(destination, resDirName).mkdir()) {
                            print("Failed to create directory")
                            return
                        }
                    } else {
                        val buff = ByteArray(BUFFER_SIZE)
                        var dest: BufferedOutputStream? = null
                        try {
                            val resFileNme = if (rootEntryDir == null) entryName else entryName.substring(rootEntryDir!!.length)
                            val fos = FileOutputStream(File(destination, resFileNme))
                            dest = BufferedOutputStream(fos, BUFFER_SIZE)
                            var count = zis.read(buff, 0, BUFFER_SIZE)
                            while (count != -1) {
                                dest.write(buff, 0, count)
                                count = zis.read(buff, 0, BUFFER_SIZE)
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

/**
 * zip a given resource
 */
fun makeZip(type: ResourceType, id: UUID): File {
    val dir = "${getBaseDir(type)}$id$SEP"
    val zipName = dir + ZIP_NAME
    val zos = ZipOutputStream(FileOutputStream(zipName))
    zipDir(dir, zos)
    zos.close()
    return File(zipName)
}


@KtorExperimentalAPI
suspend fun ByteReadChannel.writeToFile(outFile: File) = copyAndClose(outFile.writeChannel())

private fun zipDir(dir: String, zos: ZipOutputStream, pathInDir: String = "") {
    val zipDir = File(dir)
    val ls = zipDir.list { f, s ->
        !(pathInDir == "" && (s == HIVEMIND_FILE || s == ZIP_NAME))
    }
    val readBuffer = ByteArray(BUFFER_SIZE)
    var bytesIn = 0
    for (i in ls!!.indices) {
        val f = File(zipDir, ls[i])
        if (f.isDirectory) {
            val filePath = f.path
            zipDir(filePath, zos, pathInDir + "$SEP${f.name}")
        } else {
            val fis = FileInputStream(f)
            val anEntry = ZipEntry(pathInDir + SEP + f.name)
            zos.putNextEntry(anEntry)
            bytesIn = fis.read(readBuffer)
            while (bytesIn != -1) {
                zos.write(readBuffer, 0, bytesIn)
                bytesIn = fis.read(readBuffer)
            }
            fis.close()
        }
    }
}



fun main(args: Array<String>) {
    val type = ResourceType.MODEL
    val id = UUID.fromString("1d722019-c892-44bc-844b-eb5708d55987")
    println(makeZip(type, id))
}
