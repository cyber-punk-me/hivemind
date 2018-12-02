package io.cyber.hivemind

import io.vertx.core.buffer.Buffer
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by kyr7 on 30/06/2018.
 */

enum class Verb {
    POST, GET, DELETE, FIND, APPLY
}

enum class Type {
    DATA, MODEL, SCRIPT
}

enum class RunState {
    NEW, TRAINING, SERVING, ERROR
}

data class Model(val dataId: UUID, val scriptId: UUID)

data class Command(val type: Type, val verb: Verb, val buffer: Buffer? = null)

data class Meta(val scriptId: UUID?, val modelId: UUID?, val dataId: UUID?,
                val state: RunState?, val startTime: Date?, val endTime: Date?) {
    constructor(scriptId: String?, modelId: String?, dataId: String?, state: RunState, startTime: Date?, endTime: Date?)
            : this(scriptId.toUUID(), modelId.toUUID(), dataId.toUUID(), state, startTime, endTime)
}

fun String?.toUUID() : UUID? = if (isNullOrEmpty()) null else UUID.fromString(this)


class MetaList() : ArrayList<Meta>() {
    constructor(addUs : Collection<Meta>) : this() {
        addAll(addUs)
    }
}
