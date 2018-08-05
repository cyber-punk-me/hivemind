package io.cyber.hivemind.service

import io.cyber.hivemind.Command
import io.cyber.hivemind.Verb
import io.vertx.core.*
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import java.util.*

class MLVerticle : AbstractVerticle() {

    var mLService: MLService? = null

    override fun init(vertx: Vertx, context: Context) {
        super.init(vertx, context)
        mLService = MLServiceImpl(vertx)
    }

    private var consumer: MessageConsumer<Command>? = null

    override fun start(startFuture: Future<Void>) {
        consumer = vertx.eventBus().consumer<Command>(MLVerticle::class.java.name) { message ->
            val metaHeader = message.headers()["meta"]
            val idHeader = message.headers()["id"]

            if (metaHeader != null || idHeader != null) {
                val command = message.body()
                when (command.verb) {
                    Verb.APPLY -> {
                        val id = idHeader?.let{ UUID.fromString(idHeader)}
                        mLService?.applyData(id!!, command.buffer!!.toJsonObject(),
                                Handler{ jsonRes: AsyncResult<JsonObject>  ->
                                    message.reply(jsonRes.result())
                                })
                    }
                    else -> {
                    }
                }
            }
        }
    }

    override fun stop(stopFuture: Future<Void>) {
        consumer!!.unregister(stopFuture)
    }
}