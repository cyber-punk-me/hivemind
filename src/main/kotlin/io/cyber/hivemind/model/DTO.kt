package io.cyber.hivemind.model

import java.util.*

/**
 * Created by kyr7 on 30/06/2018.
 */

enum class ResourceType {
    DATA, MODEL, SCRIPT
}

enum class RunState {
    NEW, TRAINING, SERVING, ERROR
}

data class ScriptMeta(val scriptId: UUID, val created: Date)

data class DataMeta(val dataId: UUID, val created: Date, val updated: Date)

data class ModelMeta(val scriptId: UUID, val modelId: UUID, val dataId: UUID,
                     val state: RunState, val startTime: Date, val trainedTime: Date? = null, val log: String? = null) {
    constructor(scriptId: String, modelId: String, dataId: String, state: RunState, startTime: Date) :
            this(scriptId.toUUID(), modelId.toUUID(), dataId.toUUID(), state, startTime)
}

data class TrainModelReq(
        val scriptId: String,
        val dataId: String
)

fun String.toUUID(): UUID = UUID.fromString(this)
