package io.cyber.hivemind.service

import io.cyber.hivemind.Meta
import io.cyber.hivemind.MetaList
import io.cyber.hivemind.Type
import io.cyber.hivemind.constant.*
import io.cyber.hivemind.util.getNextFileName
import io.cyber.hivemind.util.unzipData
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.file.FileSystem
import java.io.File
import java.util.*

/**
 * User: kirillskiy
 * Date: 22/07/2018
 * Time: 13:56
 */
interface FileService {
    fun getName(type: Type, id: UUID): String
    fun store(type: Type, id: UUID, uploadedFile: Buffer, extension: String?, handler: Handler<AsyncResult<Meta>>)
    fun delete(type: Type, id: UUID): String
    fun find(type: Type, meta: Meta): MetaList
}

//todo implement file system watchdog for local dev
class DiskFileServiceImpl(val vertx: Vertx) : FileService {

    private val fs: FileSystem = vertx.fileSystem()

    override fun store(type: Type, id: UUID, uploadedFile: Buffer, extension: String?, handler: Handler<AsyncResult<Meta>>) {
        val baseDir = when (type) {
            Type.DATA -> LOCAL_DATA
            Type.MODEL -> LOCAL_MODEL
            Type.SCRIPT -> LOCAL_SCRIPT
        }
        val dir = "$baseDir$id$SEP"
        fs.readDir(dir) { event: AsyncResult<MutableList<String>>? ->
            if (null == event?.result()) {
                fs.mkdir(dir) {
                    storeBuffer(type, id, dir, uploadedFile, extension, handler)
                }
            } else {
                storeBuffer(type, id, dir, uploadedFile, extension, handler)
            }
        }
    }

    private fun storeBuffer(type: Type, id: UUID, dir: String, file: Buffer, extension: String?, handler: Handler<AsyncResult<Meta>>) {
        if (Type.DATA == type) {
            fs.readDir(dir) { event: AsyncResult<MutableList<String>>? ->
                val fileName = getNextFileName(event?.result(), extension)
                val path = "$dir$fileName"
                fs.writeFile(path, file) { ar ->
                    handler.handle(ar.map { _ -> Meta(null, null, id, null, null, Date()) })
                }
            }
        } else if (Type.SCRIPT == type) {
            val tempZip = "$dir.zip"
            fs.writeFile(tempZip, file) { ar ->
                        unzipData(File("$LOCAL_SCRIPT$id"), tempZip)
                handler.handle(ar.map { _ -> Meta(id, null, null, null, null, Date()) })
            }
        }
    }

    override fun delete(type: Type, id: UUID): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun find(type: Type, meta: Meta): MetaList {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getName(type: Type, id: UUID): String {
        val baseDir = when (type) {
            Type.DATA -> LOCAL_DATA
            Type.MODEL -> LOCAL_MODEL
            Type.SCRIPT -> LOCAL_SCRIPT
        }
        return "${baseDir}id"
    }


}
