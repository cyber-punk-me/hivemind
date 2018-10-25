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
    NEW, RUNNING, COMPLETE, ERROR
}

data class Model(val dataId: UUID, val scriptId: UUID)

data class Command(val type: Type, val verb: Verb, val buffer: Buffer? = null)

data class Meta(val scriptId: UUID?, val modelId: UUID?, val dataId: UUID?,
                val state: RunState?, val startTime: Date?, val endTime: Date?)

class MetaList : ArrayList<Meta>()
