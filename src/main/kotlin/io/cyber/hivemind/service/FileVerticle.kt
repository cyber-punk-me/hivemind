package io.cyber.hivemind.service

import io.cyber.hivemind.Command
import io.cyber.hivemind.constant.*
import io.cyber.hivemind.Meta
import io.cyber.hivemind.MetaList
import io.cyber.hivemind.Verb
import io.cyber.hivemind.util.fromJson
import io.vertx.core.*
import io.vertx.core.eventbus.MessageConsumer
import java.util.*

/**
 * User: kirillskiy
 * Date: 22/07/2018
 * Time: 14:09
 */
class FileVerticle : AbstractVerticle() {

    var fileService: FileService? = null

    override fun init(vertx: Vertx, context: Context) {
        super.init(vertx, context)
        fileService = DiskFileServiceImpl(vertx)
    }

    private var consumer: MessageConsumer<Command>? = null

    override fun start(startFuture: Future<Void>) {
        consumer = vertx.eventBus().consumer<Command>(FileVerticle::class.java.name) { message ->
            val idHeader = message.headers()[ID]
            val command = message.body()
            when (command.verb) {
                Verb.GET -> {
                    val id = UUID.fromString(idHeader)
                    fileService?.getZip(command.type, id!!, false, Handler { zipRes: AsyncResult<String> ->
                        message.reply(zipRes.result())
                    })
                }
                Verb.POST -> {
                    val id = UUID.fromString(idHeader)
                    val extension = message.headers()[EXT]
                    fileService?.store(command.type, id!!, command.buffer!!, extension, Handler { metaRes: AsyncResult<Meta> ->
                        message.reply(metaRes.result())
                    })
                }
                Verb.DELETE -> {
                    val id = UUID.fromString(idHeader)
                    message.reply(fileService?.delete(command.type, id!!, Handler { metaRes: AsyncResult<Void> ->
                        message.reply(metaRes.result())
                    }))
                }
                Verb.FIND -> {
                    val metaHeader = message.headers()[META]
                    val meta = fromJson(metaHeader, Meta::class.java)
                    message.reply(fileService?.find(command.type, meta, Handler { metaRes: AsyncResult<MetaList> ->
                        message.reply(metaRes.result())
                    }))
                }
                else -> {
                }
            }
        }
    }

    override fun stop(stopFuture: Future<Void>) {
        consumer!!.unregister(stopFuture)
    }
}