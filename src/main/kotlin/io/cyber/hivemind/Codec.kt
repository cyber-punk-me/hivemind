package io.cyber.hivemind

import io.cyber.hivemind.util.fromJson
import io.cyber.hivemind.util.toJson
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject

/**
 * User: kirillskiy
 * Date: 28/07/2018
 * Time: 01:50
 */

class CommandCodec : MessageCodec<Command, Command> {

    companion object {
        private const val TYPE = "t"
        private const val VERB = "v"
        private const val BUFFER_LENGTH = "b"
    }

    override fun encodeToWire(buffer: Buffer, command: Command) {
        val jsonToEncode = JsonObject()
        jsonToEncode.put(TYPE, command.type)
        jsonToEncode.put(VERB, command.verb)
        val hasBuffer = null != command.buffer
        if (hasBuffer) {
            jsonToEncode.put(BUFFER_LENGTH, command.buffer?.length())
        }
        val jsonToStr = jsonToEncode.encode()
        // Length of JSON: is NOT characters count
        val jsonLength = jsonToStr.toByteArray().size
        buffer.appendInt(jsonLength)
        buffer.appendString(jsonToStr)
        if (hasBuffer) {
            buffer.appendBuffer(command.buffer)
        }
    }

    override fun decodeFromWire(position: Int, buffer: Buffer): Command {
        val jsonLength = buffer.getInt(position)
        // Jump 4 because getInt() == 4 bytes
        val jsonStart = position + 4
        val jsonEnd = jsonStart + jsonLength
        val jsonStr = buffer.getString(jsonStart, jsonEnd)
        val contentJson = JsonObject(jsonStr)
        val type = contentJson.getString(TYPE)
        val verb = contentJson.getString(VERB)
        val bufLength = contentJson.getInteger(BUFFER_LENGTH)
        val cmdBuffer = if (bufLength != null) {
            buffer.getBuffer(jsonEnd, jsonEnd + bufLength)
        } else {
            null
        }
        return Command(Type.valueOf(type), Verb.valueOf(verb), cmdBuffer)
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

}

class MetaCodec : MessageCodec<Meta, Meta> {

    override fun encodeToWire(buffer: Buffer, meta: Meta) {
        val jsonToStr = toJson(meta)
        val length = jsonToStr.toByteArray().size
        buffer.appendInt(length)
        buffer.appendString(jsonToStr)
    }

    override fun decodeFromWire(position: Int, buffer: Buffer): Meta {
        val length = buffer.getInt(position)
        val jsonStart = position + 4
        val jsonStr = buffer.getString(jsonStart, jsonStart + length)
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

    override fun encodeToWire(buffer: Buffer, metaList: MetaList) {
        val jsonToStr = toJson(metaList)
        val length = jsonToStr.toByteArray().size
        buffer.appendInt(length)
        buffer.appendString(jsonToStr)
    }

    override fun decodeFromWire(position: Int, buffer: Buffer): MetaList {
        val length = buffer.getInt(position)
        val jsonStart = position + 4
        val jsonStr = buffer.getString(jsonStart, jsonStart + length)
        return fromJson(jsonStr, MetaList::class.java)
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
