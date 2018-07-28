package io.cyber.hivemind.service

import io.cyber.hivemind.Command
import io.cyber.hivemind.Verb
import io.vertx.core.AbstractVerticle
import io.vertx.core.Context
import io.vertx.core.Future
import io.vertx.core.Vertx
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
        fileService = DiskFileServiceImpl::class.java.newInstance()
    }

    private var consumer: MessageConsumer<Command>? = null

    override fun start(startFuture: Future<Void>) {
        consumer = vertx.eventBus().consumer<Command>(FileVerticle::class.java.name) { message ->
            val metaS = message.headers()["meta"]
            val idHeader = message.headers()["id"]
            val id = idHeader?.let{UUID.fromString(idHeader)}
            if (metaS != null || id != null) {
                val command = message.body()
                when (command.verb) {
                    Verb.GET -> {
                        message.reply(fileService?.getName(command.type, id!!))
                    }
                    Verb.POST -> {
                        message.reply(command.buffer?.let { fileService?.store(command.type, id!!, it) })
                    }
                    Verb.DELETE -> TODO()
                    Verb.FIND -> TODO()
                    Verb.APPLY -> TODO()
                }
            }
        }
    }

    override fun stop(stopFuture: Future<Void>) {
        consumer!!.unregister(stopFuture)
    }
}