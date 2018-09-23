package io.cyber.hivemind

import io.cyber.hivemind.net.DataController
import io.cyber.hivemind.net.ModelController
import io.cyber.hivemind.net.ScriptController
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

class WebVerticle : AbstractVerticle() {

    override fun start() {

        vertx.eventBus().registerDefaultCodec(Command::class.java, CommandCodec())
        vertx.eventBus().registerDefaultCodec(Meta::class.java, MetaCodec())
        vertx.eventBus().registerDefaultCodec(RunStatus::class.java, RunStatusCodec())

        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
        val modelController = ModelController(vertx)
        router.post("/model/find").handler(modelController::findModel)
        router.get("/model/:modelId").handler(modelController::getModel)
        router.post("/model/:modelId").handler(modelController::postModel)
        router.delete("/model/:modelId").handler(modelController::deleteModel)
        router.post("/apply/:modelId/:dataId").handler(modelController::applyModelToData)
        router.post("/apply/:modelId").handler(modelController::applyModelToInput)

        val dataController = DataController(vertx)
        router.post("/data/find").handler(dataController::findData)
        router.get("/data/:dataId").handler(dataController::getData)
        router.post("/data/:dataId").handler(dataController::postData)
        router.delete("/data/:dataId").handler(dataController::deleteData)

        val scriptController = ScriptController(vertx)
        router.post("/script/find").handler(scriptController::findScript)
        router.get("/script/:scriptId").handler(scriptController::getScript)
        router.post("/script/:scriptId").handler(scriptController::postScript)
        router.delete("/script/:scriptId").handler(scriptController::deleteScript)

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
