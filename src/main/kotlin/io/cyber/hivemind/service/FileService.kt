package io.cyber.hivemind.service

import io.cyber.hivemind.Meta
import io.cyber.hivemind.Type
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import java.util.*

/**
 * User: kirillskiy
 * Date: 22/07/2018
 * Time: 13:56
 */
interface FileService {
    fun getName(type: Type, id: UUID) : String
    fun store(type: Type, id: UUID, uploadedFile : Buffer) : Meta
    fun delete(type: Type, id: UUID): String
    fun find(type: Type, meta: Meta): List<Meta>
}

class DiskFileServiceImpl(val vertx: Vertx) : FileService {
    override fun store(type: Type, id: UUID, uploadedFile: Buffer): Meta {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
