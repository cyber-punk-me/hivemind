package io.cyber.hivemind.service

import io.cyber.hivemind.RunStatus
import io.vertx.core.json.JsonObject
import java.util.*

interface MLService {
    fun train(scriptId : UUID, dataId : List<UUID>) : RunStatus
    fun applyData(modelId : UUID, dataId : List<UUID>) : RunStatus
    fun applyData(modelId : UUID, dataId : JsonObject) : JsonObject
}

class MLServiceImpl : MLService {
    override fun train(scriptId: UUID, dataId: List<UUID>): RunStatus {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun applyData(modelId: UUID, dataId: List<UUID>): RunStatus {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun applyData(modelId: UUID, dataId: JsonObject): JsonObject {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}