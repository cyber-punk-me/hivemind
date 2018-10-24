package io.cyber.hivemind.service

import io.cyber.hivemind.Command
import io.cyber.hivemind.Meta
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
            val command = message.body()
            when (command.verb) {
                Verb.APPLY -> {
                    val modelIdHeader = message.headers()["modelId"]
                    val modelId = UUID.fromString(modelIdHeader)
                    mLService.applyData(modelId!!, command.buffer!!.toJsonObject(),
                            Handler { jsonRes: AsyncResult<JsonObject> ->
                                message.reply(jsonRes.result())
                            })
                }
                Verb.POST -> {
                    val modelIdHeader = message.headers()["modelId"]
                    val modelId = UUID.fromString(modelIdHeader)
                    val model = fromJson(command.buffer!!, Model::class.java)
                    val gpuHeader = message.headers()["gpu"]
                    val gpuTrain = gpuHeader?.toBoolean() ?: false
                    message.reply(mLService.train(model.scriptId, modelId, model.dataId, gpuTrain))
                }
                Verb.FIND -> {
                    val meta = fromJson(command.buffer!!, Meta::class.java)
                    message.reply(mLService.find(meta))
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