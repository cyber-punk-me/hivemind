package io.cyber.hivemind.service

import io.cyber.hivemind.Meta
import io.cyber.hivemind.Type
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
}

class DiskFileServiceImpl : FileService {
    override fun getName(type: Type, id: UUID): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun store(type: Type, id: UUID, uploadedFile: Buffer): Meta {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
