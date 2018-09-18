package io.cyber.hivemind.service

import io.cyber.hivemind.Meta
import io.cyber.hivemind.Type
import io.cyber.hivemind.util.getNextFileName
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.file.FileSystem
import java.util.*

/**
 * User: kirillskiy
 * Date: 22/07/2018
 * Time: 13:56
 */
interface FileService {
    fun getName(type: Type, id: UUID): String
    fun store(type: Type, id: UUID, uploadedFile: Buffer, handler: Handler<AsyncResult<Meta>>)
    fun delete(type: Type, id: UUID): String
    fun find(type: Type, meta: Meta): List<Meta>
}

//todo implement file system watchdog for local dev
class DiskFileServiceImpl(val vertx: Vertx) : FileService {

    companion object {
        const val ROOT = "local"
        const val DATA_ROOT = "$ROOT/data"
        const val MODEL_ROOT = "$ROOT/model"
        const val SCRIPT_ROOT = "$ROOT/script"
    }

    private val fs: FileSystem = vertx.fileSystem()

    override fun store(type: Type, id: UUID, uploadedFile: Buffer, handler: Handler<AsyncResult<Meta>>) {
        val baseDir = when (type) {
            Type.DATA -> DATA_ROOT
            Type.MODEL -> MODEL_ROOT
            Type.SCRIPT -> SCRIPT_ROOT
        }
        val dir = "$baseDir/$id"
        fs.readDir(dir) { event: AsyncResult<MutableList<String>>? ->
            if (null == event?.result()) {
                fs.mkdir(dir) {
                    storeBuffer(type, id, dir, uploadedFile, handler)
                }
            } else {
                storeBuffer(type, id, dir, uploadedFile, handler)
            }
        }
    }

    private fun storeBuffer(type: Type, id: UUID, dir: String, file: Buffer, handler: Handler<AsyncResult<Meta>>) {
        if (Type.DATA == type) {
            fs.readDir(dir) { event: AsyncResult<MutableList<String>>? ->
                val fileName = getNextFileName(event?.result())
                val path = "$dir/$fileName"
                fs.writeFile(path, file) { ar ->
                    handler.handle(ar.map { _ -> Meta(id, fileName, null, path, null, System.currentTimeMillis(), null) })
                }
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
