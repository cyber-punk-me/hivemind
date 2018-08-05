package io.cyber.hivemind.service

import io.cyber.hivemind.Meta
import io.cyber.hivemind.Type
import io.cyber.hivemind.util.getNextFileName
import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.file.FileSystem
import java.io.IOException
import java.util.*

/**
 * User: kirillskiy
 * Date: 22/07/2018
 * Time: 13:56
 */
interface FileService {
    fun getName(type: Type, id: UUID): String
    fun store(type: Type, id: UUID, uploadedFile: Buffer)
    fun delete(type: Type, id: UUID): String
    fun find(type: Type, meta: Meta): List<Meta>
}

//todo implement file system watchdog for local dev
class DiskFileServiceImpl(val vertx: Vertx) : FileService {

    var ROOT = "data"
    var DATA_ROOT = ROOT + "/data"
    var MODEL_ROOT = ROOT + "/model"
    var SCRIPT_ROOT = ROOT + "/script"

    var fs = vertx.fileSystem()

    override fun store(type: Type, id: UUID, uploadedFile: Buffer) {
        val baseDir = when (type) {
            Type.DATA -> DATA_ROOT
            Type.MODEL -> MODEL_ROOT
            Type.SCRIPT -> SCRIPT_ROOT
        }
        val dir = "$baseDir/$id"
        fs.readDir(dir) { event: AsyncResult<MutableList<String>>? ->
            if (null == event?.result()) {
                fs.mkdir(dir) {
                    storeBuffer(type, dir, uploadedFile)
                }
            } else {
                storeBuffer(type, dir, uploadedFile)
            }
        }
    }

    private fun storeBuffer(type: Type, dir: String, file: Buffer) {
        if (Type.DATA == type) {
            fs.readDir(dir) { event: AsyncResult<MutableList<String>>? ->
                val fileName = getNextFileName(event?.result())
                fs.writeFile("$dir/$fileName", file, {})
            }
        }
    }

    override fun delete(type: Type, id: UUID): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun find(type: Type, meta: Meta): List<Meta> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getName(type: Type, id: UUID): String {
        return "local/data/test.zip"
    }


}
