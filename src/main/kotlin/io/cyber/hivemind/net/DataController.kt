package io.cyber.hivemind.net

import io.cyber.hivemind.Command
import io.cyber.hivemind.Meta
import io.cyber.hivemind.ResourceType
import io.cyber.hivemind.Verb
import io.cyber.hivemind.constant.*
import io.cyber.hivemind.service.FileVerticle
import io.cyber.hivemind.util.addNotNullHeader
import io.cyber.hivemind.util.toJson
import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.ext.web.RoutingContext


class DataController(vertx: Vertx) : Controller(vertx, ResourceType.DATA) {

    fun getData(context: RoutingContext) = getZip(context)

    fun postData(context: RoutingContext) {
        val cmd = Command(ResourceType.DATA, Verb.POST, context.body)
        val opts = DeliveryOptions()
        val dataId = context.request().getParam(ID)
        val ext = context.request().getParam(EXT)
        opts.addHeader(ID, dataId)
        opts.addNotNullHeader(EXT, ext)
        vertx.eventBus().send(FileVerticle::class.java.name, cmd, opts) { ar: AsyncResult<Message<Meta>> ->
            if (ar.succeeded()) {
                context.response().end(toJson(ar.result().body()))
            } else {
                ar.cause().printStackTrace()
            }
            context.response().close()
        }
    }


    fun findData(context: RoutingContext) = find(context)

    fun deleteData(context: RoutingContext) = delete(context)

}
