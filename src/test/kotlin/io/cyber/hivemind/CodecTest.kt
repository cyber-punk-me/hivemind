package io.cyber.hivemind

import io.vertx.core.buffer.Buffer
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class CodecTest {

    @Test
    fun testCommand(tc: TestContext) {
        val command = Command(ResourceType.MODEL, Verb.FIND, Buffer.buffer("hello"))
        val transport = Buffer.buffer()
        val codec = CommandCodec()
        codec.encodeToWire(transport, command)
        val decoded = codec.decodeFromWire(0, transport)
        assertEquals(command, decoded)
    }

    @Test
    fun testCommandEmpty(tc: TestContext) {
        val command = Command(ResourceType.MODEL, Verb.FIND, null)
        val transport = Buffer.buffer()
        val codec = CommandCodec()
        codec.encodeToWire(transport, command)
        val decoded = codec.decodeFromWire(0, transport)
        assertEquals(command, decoded)
    }

}