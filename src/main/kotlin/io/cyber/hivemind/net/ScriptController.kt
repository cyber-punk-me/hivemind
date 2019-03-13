package io.cyber.hivemind.net

import io.cyber.hivemind.Type
import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext

class ScriptController(vertx: Vertx) : Controller(vertx, Type.SCRIPT) {

    fun getScript(context: RoutingContext) = getZip(context)

    fun postScript(context: RoutingContext) = postZip(context)

    fun deleteScript(context: RoutingContext) = delete(context)

    fun findScript(context: RoutingContext) = find(context)

}
