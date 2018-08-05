package io.cyber.hivemind

import io.vertx.core.buffer.Buffer
import java.util.*

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

data class Meta(val id: UUID?, val name: String?, val note: String?,
                val path: String?, val error: String?, val time: Long, val tags: List<String>?)

data class Command(val type: Type, val verb: Verb, val buffer: Buffer? = null)

data class RunStatus(val state : RunState, val startTime : Date, val endTime : Date,
                     val scriptId : UUID, val modelId : UUID, val dataId : List<UUID>)

