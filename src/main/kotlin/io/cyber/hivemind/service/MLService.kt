package io.cyber.hivemind.service

import io.cyber.hivemind.RunStatus
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import java.util.*

interface MLService {
    fun train(scriptId: UUID, dataId: List<UUID>): RunStatus
    fun runData(modelId: UUID, dataId: List<UUID>): RunStatus
    fun applyData(modelId: UUID, json: JsonObject, handler: Handler<AsyncResult<JsonObject>>)
}

class MLServiceImpl(val vertx: Vertx) : MLService {

    val TF_SERVABlE_HOST = "localhost"
    val TF_SERVABlE_PORT = 8501
    val TF_SERVABlE_URI = "/v1/models/half_plus_three:predict"
    val client = WebClient.create(vertx)

    override fun train(scriptId: UUID, dataId: List<UUID>): RunStatus {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun runData(modelId: UUID, dataId: List<UUID>): RunStatus {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun applyData(modelId: UUID, json: JsonObject, handler: Handler<AsyncResult<JsonObject>>) {
        client.post(TF_SERVABlE_PORT, TF_SERVABlE_HOST, TF_SERVABlE_URI)
                .sendJsonObject(json) { ar ->
                    run {
                        handler.handle(ar.map { http -> http.bodyAsJsonObject() })
                    }
                }
    }

}