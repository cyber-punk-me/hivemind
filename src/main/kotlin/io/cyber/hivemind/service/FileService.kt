package io.cyber.hivemind.service

import io.cyber.hivemind.Meta
import io.cyber.hivemind.MetaList
import io.cyber.hivemind.Type
import io.cyber.hivemind.constant.*
import io.cyber.hivemind.util.fromJson
import io.cyber.hivemind.util.getNextFileName
import io.cyber.hivemind.util.toJson
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
    fun getZip(type: Type, id: UUID, handler: Handler<AsyncResult<String>>)
    fun store(type: Type, id: UUID, uploadedFile: Buffer, extension: String?, handler: Handler<AsyncResult<Meta>>)
    fun find(type: Type, meta: Meta, handler: Handler<AsyncResult<MetaList>>): MetaList
    fun delete(type: Type, id: UUID)
}

class DiskFileServiceImpl(val vertx: Vertx) : FileService {

    private val fs: FileSystem = vertx.fileSystem()

    override fun store(type: Type, id: UUID, uploadedFile: Buffer, extension: String?, handler: Handler<AsyncResult<Meta>>) {
        val baseDir = getBaseDir(type)
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
            storeDataBuffer(dir, id, file, extension, handler)
        } else if (Type.SCRIPT == type) {
            storeScriptBuffer(dir, id, file, handler)
        }
    }

    private fun storeScriptBuffer(dir: String, id: UUID, file: Buffer, handler: Handler<AsyncResult<Meta>>) {
        val startTime = Date()
        val tempZip = "$dir.zip"
        try {
            fs.deleteRecursiveBlocking(dir, true)
        } catch (t : Throwable) {
            //ok, nothing to cleanup
        }
        fs.mkdirsBlocking(dir)
        fs.writeFile(tempZip, file) { ar ->
            unzipData(File("$LOCAL_SCRIPT$id"), tempZip)
            handler.handle(ar.map { _ ->
                val meta = Meta(id, null, null, null, startTime, Date())
                updateMeta(Type.SCRIPT, id, meta)
                meta
            })
        }
    }

    private fun storeDataBuffer(dir: String, id: UUID, file: Buffer, extension: String?, handler: Handler<AsyncResult<Meta>>) {
        val startTime = Date()
        val files = fs.readDirBlocking(dir)
        val fileName = getNextFileName(files, extension)
        val path = "$dir$fileName"
        fs.writeFile(path, file) { ar ->
            handler.handle(ar.map { _ ->
                val prevMeta = getMeta(Type.DATA, id)
                val meta = Meta(null, null, id, null, prevMeta?.startTime ?: startTime, Date())
                updateMeta(Type.DATA, id, meta)
                meta
            })
        }
    }

    override fun delete(type: Type, id: UUID) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun find(type: Type, meta: Meta, handler: Handler<AsyncResult<MetaList>>): MetaList {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    //zips files and provides the name of resulting zip file
    override fun getZip(type: Type, id: UUID, handler: Handler<AsyncResult<String>>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun getBaseDir(type: Type): String {
        return when (type) {
            Type.DATA -> LOCAL_DATA
            Type.MODEL -> LOCAL_MODEL
            Type.SCRIPT -> LOCAL_SCRIPT
        }
    }

    private fun getMeta(type: Type, id: UUID): Meta? {
        val metaFile = "${getBaseDir(type)}$id$META_LOCATION"
        val metaBuf = try {
            fs.readFileBlocking(metaFile)
        } catch (e: Throwable) {
            null
        }
        return metaBuf?.let { fromJson(it, Meta::class.java) }
    }

    private fun updateMeta(type: Type, id: UUID, meta: Meta, cleanUp: Boolean = false) {
        val metaLocation = "${getBaseDir(type)}$id$HIVEMIND_DIR"
        if (cleanUp) {
            fs.deleteRecursiveBlocking(metaLocation, true)
        }
        fs.mkdirsBlocking(metaLocation)
        val metaBuf = toJson(meta)
        fs.writeFileBlocking("$metaLocation$SEP$META", Buffer.buffer(metaBuf))
    }


}
