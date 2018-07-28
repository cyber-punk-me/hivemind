package io.cyber.hivemind

import io.vertx.core.buffer.Buffer


/**
 * Created by kyr7 on 30/06/2018.
 */

enum class Verb {
   POST, GET, DELETE, FIND, APPLY
}

enum class Type {
   DATA, MODEL, SCRIPT
}

data class Meta(val id: String?, val name: String?, val note: String?,
                val path: String?, val error: String?, val time: Long, val tags: List<String>?)

data class Command(val type: Type, val verb: Verb, val buffer: Buffer? = null)

