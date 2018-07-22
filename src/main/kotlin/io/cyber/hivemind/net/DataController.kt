package io.cyber.hivemind.net

import io.cyber.hivemind.Command
import io.cyber.hivemind.Meta
import io.cyber.hivemind.Type
import io.cyber.hivemind.Verb
import io.cyber.hivemind.service.FileVerticle
import io.cyber.hivemind.util.downloadFile
import io.cyber.hivemind.util.toJson
import io.cyber.hivemind.util.uploadFile
import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.ext.web.RoutingContext

class DataController(val vertx: Vertx) {

    fun getData(context: RoutingContext) {
        val dataId = context.request().getParam("dataId")
        val cmd = Command(Type.DATA, Verb.GET, HashMap<String, String>().apply { this["id"] = dataId })
        vertx.eventBus().send(FileVerticle.FILE_VERTICLE, cmd) { ar: AsyncResult<Message<String>>? ->
            if (ar!!.succeeded()) {
                downloadFile(context, ar.result().body())
            } else {
                ar.cause().printStackTrace()
            }
            context.response().close()
        }
    }

    fun postData(context: RoutingContext) {
        val dataId = context.request().getParam("dataId")
        val file = uploadFile(context, vertx)
        val cmd = Command(Type.DATA, Verb.POST, HashMap<String, String>().apply { this["id"] = dataId }, buffer = file)
        vertx.eventBus().send(FileVerticle.FILE_VERTICLE, cmd) { ar: AsyncResult<Message<Meta>>? ->
            if (ar!!.succeeded()) {
                context.response().write(toJson(ar.result().body()))
            } else {
                ar.cause().printStackTrace()
            }
            context.response().close()
        }
    }

    fun deleteData(context: RoutingContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun findData(context: RoutingContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
