package io.cyber.hivemind.net

import io.cyber.hivemind.Command
import io.cyber.hivemind.Type
import io.cyber.hivemind.Verb
import io.cyber.hivemind.service.FileVerticle
import io.cyber.hivemind.service.MLVerticle
import io.cyber.hivemind.util.downloadFile
import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.ext.web.RoutingContext


/**
 * User: kirillskiy
 * Date: 22/07/2018
 * Time: 01:20
 */
class ModelController(val vertx: Vertx) {

    fun getModel(routingContext: RoutingContext) {
        val modelId = routingContext.request().getParam("modelId")
    }

    fun postModel(context: RoutingContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun deleteModel(context: RoutingContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun findModel(context: RoutingContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun applyModelToData(context: RoutingContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun applyModelToInput(context: RoutingContext) {
        val cmd = Command(Type.MODEL, Verb.APPLY, context.body)
        val opts = DeliveryOptions()
        vertx.eventBus().send(MLVerticle::class.java.name, cmd, opts) { ar: AsyncResult<Message<String>>? ->
            if (ar!!.succeeded()) {
                context.response().end(ar.result().body())
            } else {
                ar.cause().printStackTrace()
                context.response().end(ar.toString())
            }
        }
    }
}