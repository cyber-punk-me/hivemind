package io.cyber.hivemind.util

import io.cyber.hivemind.Type
import io.cyber.hivemind.constant.HIVEMIND_FILE
import io.cyber.hivemind.constant.SEP
import io.cyber.hivemind.constant.getBaseDir
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.impl.FutureFactoryImpl
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

fun getNextFileName(files: List<String>?, extension: String?): String {
    val extRes = extension?.let { ".$extension" } ?: ""
    val defaultName = "0$extRes"
    if (files == null || files.isEmpty()) return defaultName
    val maxFile: Int? = files.map { f -> f.split(File.separator).last() }.map { s -> s.cutEtension() }.filter { f -> isInteger(f) }.map { s -> Integer.parseInt(s) }.max()
    return when {
        maxFile != null -> "" + (maxFile + 1) + extRes
        else -> defaultName
    }
}

private fun String.cutEtension(): String {
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

fun unzipData(directory: File, zipFileName: String) {
    BufferedInputStream(FileInputStream(zipFileName)).use { `is` ->
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
                        if (!resDirName.isEmpty() && !File(directory, resDirName).mkdir()) {
                            print("Failed to create directory")
                            return
                        }
                    } else {
                        val buff = ByteArray(BUFFER_SIZE)
                        var dest: BufferedOutputStream? = null
                        try {
                            val resFileNme = if (rootEntryDir == null) entryName else entryName.substring(rootEntryDir!!.length)
                            val fos = FileOutputStream(File(directory, resFileNme))
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


fun zipDir(dir: String, zos: ZipOutputStream, zipName: String? = null) {
    val zipDir = File(dir)
    val ls = zipDir.list { f, s -> !(f.isDirectory && s.startsWith(HIVEMIND_FILE)) }
    val readBuffer = ByteArray(BUFFER_SIZE)
    var bytesIn = 0
    for (i in ls!!.indices) {
        val f = File(zipDir, ls[i])
        if (f.name == zipName) {
            continue
        }
        if (f.isDirectory) {
            val filePath = f.path
            zipDir(filePath, zos)
        }
        val fis = FileInputStream(f)
        val anEntry = ZipEntry(f.path)
        zos.putNextEntry(anEntry)
        bytesIn = fis.read(readBuffer)
        while (bytesIn != -1) {
            zos.write(readBuffer, 0, bytesIn)
            bytesIn = fis.read(readBuffer)
        }
        fis.close()
    }
}

fun makeZip(type: Type, id: UUID, zipName: String, handler: Handler<AsyncResult<String>>) {
    try {
        val zos = ZipOutputStream(FileOutputStream(zipName))
        zipDir("${getBaseDir(type)}$id$SEP", zos)
        zos.close()
        handler.handle(FutureFactoryImpl().succeededFuture(zipName))
    } catch (e: Exception) {
        handler.handle(FutureFactoryImpl().failedFuture(e))
    }

}
