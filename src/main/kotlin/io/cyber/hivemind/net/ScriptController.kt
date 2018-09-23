package io.cyber.hivemind.net

import io.cyber.hivemind.Command
import io.cyber.hivemind.Meta
import io.cyber.hivemind.Type
import io.cyber.hivemind.Verb
import io.cyber.hivemind.service.FileVerticle
import io.cyber.hivemind.util.toJson
import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.ext.web.RoutingContext

class ScriptController(val vertx: Vertx) {
    fun getScript(routingContext: RoutingContext) {
        val scriptId = routingContext.request().getParam("scriptId")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun postScript(context: RoutingContext) {
        val upload = context.fileUploads().iterator().next()!!
        //todo no premature reading here
        val file = vertx.fileSystem().readFileBlocking(upload.uploadedFileName())
        val cmd = Command(Type.SCRIPT, Verb.POST, file)
        val opts = DeliveryOptions()
        val scriptId = context.request().getParam("scriptId")
        opts.addHeader("id", scriptId)
        vertx.eventBus().send(FileVerticle::class.java.name, cmd, opts) { ar: AsyncResult<Message<Meta>>? ->
            if (ar!!.succeeded()) {
                //write the buffer returned from FileVerticle
                context.response().end(toJson(ar.result().body()))
            } else {
                ar.cause().printStackTrace()
            }
            context.response().close()
        }
    }

    fun deleteScript(context: RoutingContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun findScript(context: RoutingContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
