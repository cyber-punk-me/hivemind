package io.cyber.hivemind.service

import io.cyber.hivemind.*
import io.cyber.hivemind.constant.MODEL_ID
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
                    val modelIdHeader = message.headers()[MODEL_ID]
                    val modelId = UUID.fromString(modelIdHeader)
                    mLService.applyData(modelId!!, command.buffer!!.toJsonObject(),
                            Handler { jsonRes: AsyncResult<JsonObject> ->
                                message.reply(jsonRes.result())
                            })
                }
                Verb.POST -> {
                    val modelIdHeader = message.headers()[MODEL_ID]
                    val modelId = UUID.fromString(modelIdHeader)
                    val model = fromJson(command.buffer!!, Model::class.java)
                    message.reply(mLService.train(model.scriptId, modelId, model.dataId))
                }
                Verb.FIND -> {
                    //todo use as search filter
                    val meta = fromJson(command.buffer!!, Meta::class.java)
                    val trainingMetas = mLService.getModelsInTraining()
                    val servingMetas = mLService.getModelsInServing()
                    trainingMetas.addAll(servingMetas)
                    message.reply(trainingMetas)
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