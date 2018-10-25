package io.cyber.hivemind

import io.cyber.hivemind.util.fromJson
import io.cyber.hivemind.util.toJson
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject
import java.util.*
import kotlin.collections.ArrayList


/**
 * User: kirillskiy
 * Date: 28/07/2018
 * Time: 01:50
 */

class CommandCodec : MessageCodec<Command, Command> {

    override fun encodeToWire(buffer: Buffer, command: Command) {
        val jsonToEncode = JsonObject()
        jsonToEncode.put("t", command.type)
        jsonToEncode.put("v", command.verb)
        command.buffer ?. let {jsonToEncode.put("b", createBufferLink(command.buffer))}
        val jsonToStr = jsonToEncode.encode()
        // Length of JSON: is NOT characters count
        val length = jsonToStr.toByteArray().size
        buffer.appendInt(length)
        buffer.appendString(jsonToStr)
    }

    override fun decodeFromWire(position: Int, buffer: Buffer): Command {
        val length = buffer.getInt(position)
        // Jump 4 because getInt() == 4 bytes
        val jsonStr = buffer.getString(position + 4, position + 4 + length)
        val contentJson = JsonObject(jsonStr)
        // Get fields
        val type = contentJson.getString("t")
        val verb = contentJson.getString("v")
        val bufIn = if (contentJson.getString("b") != null) { getBuffer(UUID.fromString(contentJson.getString("b")))} else null
        return Command(Type.valueOf(type), Verb.valueOf(verb), bufIn)
    }

    override fun transform(customMessage: Command): Command {
        return customMessage
    }

    override fun name(): String {
        return this.javaClass.simpleName
    }

    override fun systemCodecID(): Byte {
        return -1
    }


    companion object {

        private const val LINK_LIMIT = 100000
        private val bufferLocator: LinkedHashMap<UUID, Buffer> = LinkedHashMap()

        fun createBufferLink(buffer: Buffer): UUID {
            val uuid = UUID.randomUUID()
            bufferLocator[uuid] = buffer
            if (bufferLocator.size > LINK_LIMIT) {
                bufferLocator.iterator().remove()
            }
            return uuid
        }

        fun getBuffer(link: UUID?): Buffer? {
            return bufferLocator[link]
        }
    }
}

class MetaCodec : MessageCodec<Meta, Meta> {

    override fun encodeToWire(buffer: Buffer, meta: Meta) {
        val jsonToStr = toJson(meta)
        // Length of JSON: is NOT characters count
        val length = jsonToStr.toByteArray().size
        buffer.appendInt(length)
        buffer.appendString(jsonToStr)
    }

    override fun decodeFromWire(position: Int, buffer: Buffer): Meta {
        val length = buffer.getInt(position)
        // Jump 4 because getInt() == 4 bytes
        val jsonStr = buffer.getString(position + 4, position + 4 + length)
        return fromJson(jsonStr, Meta::class.java)
    }

    override fun transform(customMessage: Meta): Meta {
        return customMessage
    }

    override fun name(): String {
        return this.javaClass.simpleName
    }

    override fun systemCodecID(): Byte {
        return -1
    }

}

class MetaListCodec : MessageCodec<MetaList, MetaList> {

    override fun decodeFromWire(position: Int, buffer: Buffer): MetaList {
        val length = buffer.getInt(position)
        val jsonStr = buffer.getString(position + 4, position + 4 + length)
        return fromJson(jsonStr, MetaList::class.java)
    }

    override fun encodeToWire(buffer: Buffer, s: MetaList) {
        buffer.appendString(toJson(s))
    }

    override fun transform(customMessage: MetaList): MetaList {
        return customMessage
    }

    override fun name(): String {
        return this.javaClass.simpleName
    }

    override fun systemCodecID(): Byte {
        return -1
    }

}
