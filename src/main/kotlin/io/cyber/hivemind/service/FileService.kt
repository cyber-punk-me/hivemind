package io.cyber.hivemind.service

import io.cyber.hivemind.Meta
import io.cyber.hivemind.MetaList
import io.cyber.hivemind.Type
import io.cyber.hivemind.constant.*
import io.cyber.hivemind.util.*
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.file.FileSystem
import io.vertx.core.impl.FutureFactoryImpl
import java.io.File
import java.util.*

/**
 * User: kirillskiy
 * Date: 22/07/2018
 * Time: 13:56
 */
interface FileService {
    fun getZip(type: Type, id: UUID, refresh: Boolean = false, handler: Handler<AsyncResult<String>>)
    fun store(type: Type, id: UUID, uploadedFile: Buffer, extension: String?, handler: Handler<AsyncResult<Meta>>)
    fun find(type: Type, meta: Meta, handler: Handler<AsyncResult<MetaList>>)
    fun delete(type: Type, id: UUID, handler: Handler<AsyncResult<Void>>)
}

private const val ZIP_NAME = ".zip"

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

    private fun storeBuffer(type: Type, id: UUID, dir: String, buffer: Buffer, extension: String?, handler: Handler<AsyncResult<Meta>>) {
        when (type) {
            Type.DATA -> storeDataBuffer(dir, id, buffer, extension, handler)
            Type.SCRIPT -> storeScriptBuffer(dir, id, buffer, handler)
            Type.MODEL -> updateMeta(Type.MODEL, id, fromJson(buffer, Meta::class.java))
        }
    }

    private fun storeScriptBuffer(dir: String, id: UUID, file: Buffer, handler: Handler<AsyncResult<Meta>>) {
        val startTime = Date()
        val tempZip = "$dir$ZIP_NAME"
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

    override fun delete(type: Type, id: UUID, handler: Handler<AsyncResult<Void>> ) {
        //todo does it work?
        fs.deleteRecursive("${getBaseDir(type)}$id$SEP", true , handler)
    }

    override fun find(type: Type, meta: Meta, handler: Handler<AsyncResult<MetaList>>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    //zips files and provides the name of resulting zip file
    override fun getZip(type: Type, id: UUID, refresh: Boolean, handler: Handler<AsyncResult<String>>) {
        val zipName = "${getBaseDir(type)}$id$SEP$ZIP_NAME"
        fs.exists(zipName) { res: AsyncResult<Boolean> ->
            if (refresh && res.succeeded() && res.result()) {
                handler.handle(FutureFactoryImpl().succeededFuture(zipName))
            } else {
                makeZip(type, id, zipName, handler)
            }
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
