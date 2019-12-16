package io.cyber.hivemind.net

/*
import io.cyber.hivemind.*
import io.cyber.hivemind.constant.*
import io.cyber.hivemind.service.MLVerticle
import io.cyber.hivemind.util.toJson
import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

*/
/**
 * User: kirillskiy
 * Date: 22/07/2018
 * Time: 01:20
 *//*

class ModelController(vertx: Vertx) : Controller(vertx, ResourceType.MODEL) {

    */
/**
     * Start training process
     *//*

    fun postModel(context: RoutingContext) {
        val modelId = context.request().getParam(ID)
        val cmd = Command(ResourceType.MODEL, Verb.POST, context.body)
        val opts = DeliveryOptions()
        opts.addHeader(ID, modelId)
        vertx.eventBus().send(MLVerticle::class.java.name, cmd, opts) { ar: AsyncResult<Message<Meta>> ->
            if (ar.succeeded()) {
                context.response().end(toJson(ar.result().body()))
            } else {
                ar.cause().printStackTrace()
                context.response().end(ar.toString())
            }
        }
    }

    fun getModel(context: RoutingContext) = getZip(context)


    fun deleteModel(context: RoutingContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun findModel(context: RoutingContext) {
        val cmd = Command(type, Verb.FIND, context.body)
        vertx.eventBus().send(MLVerticle::class.java.name, cmd) { ar: AsyncResult<Message<List<Meta>>> ->
            if (ar.succeeded()) {
                context.response().end(toJson(ar.result().body()))
            } else {
                ar.cause().printStackTrace()
                context.response().end(ar.toString())
            }
        }
    }

    fun applyModelToInput(context: RoutingContext) {
        val modelId = context.request().getParam(ID)
        val cmd = Command(ResourceType.MODEL, Verb.APPLY, context.body)
        val opts = DeliveryOptions()
        opts.addHeader(ID, modelId)
        vertx.eventBus().send(MLVerticle::class.java.name, cmd, opts) { ar: AsyncResult<Message<JsonObject>>? ->
            if (ar!!.succeeded() && ar.result().body() != null) {
                context.response().end(ar.result().body().encode())
            } else {
                context.response().end(ar.toString())
            }
        }
    }
}*/
