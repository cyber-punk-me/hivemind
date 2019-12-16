package io.cyber.hivemind.net
/*

import io.cyber.hivemind.Command
import io.cyber.hivemind.Meta
import io.cyber.hivemind.ResourceType
import io.cyber.hivemind.Verb
import io.cyber.hivemind.constant.ID
import io.cyber.hivemind.service.FileVerticle
import io.cyber.hivemind.util.toJson
import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.ext.web.RoutingContext

open class Controller(val vertx: Vertx, val type: ResourceType) {

    fun postZip(context: RoutingContext) {
        val upload = context.fileUploads().iterator().next()!!
        val file = vertx.fileSystem().readFileBlocking(upload.uploadedFileName())
        val cmd = Command(type, Verb.POST, file)
        val opts = DeliveryOptions()
        val id = context.request().getParam(ID)
        opts.addHeader(ID, id)
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


    fun getZip(context: RoutingContext) {
        val id = context.request().getParam(ID)
        val cmd = Command(type, Verb.GET)
        val opts = DeliveryOptions()
        opts.addHeader(ID, id)
        vertx.eventBus().send(FileVerticle::class.java.name, cmd, opts) { ar: AsyncResult<Message<String>> ->
            if (ar.succeeded()) {
                if (ar.result() != null) {
                    context.response().sendFile(ar.result().body())
                    context.response().end()
                } else {
                    context.response().sendFile("empty file")
                    context.response().end()
                }
            } else {
                ar.cause().printStackTrace()
                context.response().end(ar.toString())
            }
        }
    }

    fun find(context: RoutingContext) {
        val cmd = Command(type, Verb.FIND, context.body)
        vertx.eventBus().send(FileVerticle::class.java.name, cmd) { ar: AsyncResult<Message<List<Meta>>> ->
            if (ar.succeeded()) {
                context.response().end(toJson(ar.result().body()))
            } else {
                ar.cause().printStackTrace()
                context.response().end(ar.toString())
            }
        }
    }


    fun delete(context: RoutingContext) {
        TODO()
    }

}
*/
