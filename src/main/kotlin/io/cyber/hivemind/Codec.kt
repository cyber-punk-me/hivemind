package io.cyber.hivemind

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject
import java.util.*


/**
 * User: kirillskiy
 * Date: 28/07/2018
 * Time: 01:50
 */
class CommandCodec : MessageCodec<Command, Command> {

    override fun encodeToWire(buffer: Buffer, command: Command) {
        // Easiest ways is using JSON object
        val jsonToEncode = JsonObject()
        jsonToEncode.put("t", command.type)
        jsonToEncode.put("v", command.verb)
        command.buffer ?. let {jsonToEncode.put("b", createBufferLink(command.buffer))}
        // Encode object to string
        val jsonToStr = jsonToEncode.encode()

        // Length of JSON: is NOT characters count
        val length = jsonToStr.toByteArray().size

        // Write data into given buffer
        buffer.appendInt(length)
        buffer.appendString(jsonToStr)
    }

    override fun decodeFromWire(position: Int, buffer: Buffer): Command {
        // Custom message starting from this *position* of buffer
        // Length of JSON
        val length = buffer.getInt(position)

        // Get JSON string by it`s length
        // Jump 4 because getInt() == 4 bytes
        val jsonStr = buffer.getString(position + 4, position + 4 + length)
        val contentJson = JsonObject(jsonStr)

        // Get fields
        val type = contentJson.getString("t")
        val verb = contentJson.getString("v")
        val bufIn = if (contentJson.getString("b") != null) { getBuffer(UUID.fromString(contentJson.getString("b")))} else null

        // We can finally create custom message object
        return Command(Type.valueOf(type), Verb.valueOf(verb), bufIn)
    }

    override fun transform(customMessage: Command): Command {
        // If a message is sent *locally* across the event bus.
        // This example sends message just as is
        return customMessage
    }

    override fun name(): String {
        // Each codec must have a unique name.
        // This is used to identify a codec when sending a message and for unregistering codecs.
        return this.javaClass.simpleName
    }

    override fun systemCodecID(): Byte {
        // Always -1
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