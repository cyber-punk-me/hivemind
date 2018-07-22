package io.cyber.hivemind.net

import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext

class ScriptController(vertx: Vertx) {
    fun getScript(routingContext: RoutingContext) {
        val scriptId = routingContext.request().getParam("scriptId")
    }

    fun postScript(context: RoutingContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun deleteScript(context: RoutingContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun findScript(context: RoutingContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
