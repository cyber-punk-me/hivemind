package io.cyber.hivemind

import io.cyber.hivemind.net.DataController
import io.cyber.hivemind.net.ModelController
import io.cyber.hivemind.net.ScriptController
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler

class WebVerticle : AbstractVerticle() {

    override fun start() {

        vertx.eventBus().registerDefaultCodec(Command::class.java, CommandCodec())
        vertx.eventBus().registerDefaultCodec(Meta::class.java, MetaCodec())
        vertx.eventBus().registerDefaultCodec(MetaList::class.java, MetaListCodec())
        vertx.eventBus().registerDefaultCodec(Unit::class.java, UnitCodec())

        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
        val modelController = ModelController(vertx)
        router.post("/model/find").handler(modelController::findModel)
        router.get("/model/:id").handler(modelController::getModel)
        router.post("/model/:id").handler(modelController::postModel)
        router.delete("/model/:id").handler(modelController::deleteModel)
        router.post("/apply/:id").handler(modelController::applyModelToInput)

        val dataController = DataController(vertx)
        router.post("/data/find").handler(dataController::findData)
        router.get("/data/:dataId").handler(dataController::getData)
        router.post("/data/:id").handler(dataController::postData)
        router.delete("/data/:id").handler(dataController::deleteData)

        val scriptController = ScriptController(vertx)
        router.post("/script/find").handler(scriptController::findScript)
        router.get("/script/:id").handler(scriptController::getScript)
        router.post("/script/:id").handler(scriptController::postScript)
        router.delete("/script/:id").handler(scriptController::deleteScript)

        router.get("/time").handler{context : RoutingContext
            -> context.response().end("${System.currentTimeMillis()}")
        }

        vertx.createHttpServer().requestHandler{ router.accept(it) }.listen(8080)
    }
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val vertx = Vertx.vertx()
            vertx.deployVerticle("io.cyber.hivemind.WebVerticle")
            vertx.deployVerticle("io.cyber.hivemind.service.FileVerticle")
            vertx.deployVerticle("io.cyber.hivemind.service.MLVerticle")
        }
    }

}

fun main(args : Array<String>) {
    WebVerticle.main(args)
}
