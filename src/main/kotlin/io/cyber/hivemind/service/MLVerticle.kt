package io.cyber.hivemind.service

import io.cyber.hivemind.Command
import io.cyber.hivemind.Model
import io.cyber.hivemind.Verb
import io.cyber.hivemind.util.fromJson
import io.vertx.core.*
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import java.util.*

class MLVerticle : AbstractVerticle() {

    lateinit var mLService: MLService

    override fun init(vertx: Vertx, context: Context) {
        super.init(vertx, context)
        mLService = MLServiceImpl(vertx)
    }

    private var consumer: MessageConsumer<Command>? = null

    override fun start(startFuture: Future<Void>) {
        consumer = vertx.eventBus().consumer<Command>(MLVerticle::class.java.name) { message ->
            val modelIdHeader = message.headers()["modelId"]

            if (modelIdHeader != null) {
                val command = message.body()
                val modelId = UUID.fromString(modelIdHeader)
                when (command.verb) {
                    Verb.APPLY -> {
                        mLService.applyData(modelId!!, command.buffer!!.toJsonObject(),
                                Handler { jsonRes: AsyncResult<JsonObject> ->
                                    message.reply(jsonRes.result())
                                })
                    }
                    Verb.POST -> {
                        val model = fromJson(command.buffer!!, Model::class.java)
                        message.reply(mLService.train(model.scriptId, modelId, model.dataId))
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