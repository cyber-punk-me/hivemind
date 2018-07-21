package io.cyber.hivemind

import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

class MainVerticle : AbstractVerticle() {

    override fun start() {

        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
        //todo implement file system watchdog for local dev
        val modelController = ModelController()
        router.get("/model/:modelId").handler(modelController::getModel)
        router.post("/model/:modelId").handler{ modelController::postModel }
        router.delete("/model/:modelId").handler{ modelController::deleteModel }
        router.post("/model/find").handler{ modelController::findModel }
        router.post("/apply/:modelId/:dataId").handler{ modelController::applyModelToData }
        router.post("/apply/:modelId").handler{ modelController::applyModelToInput }

        val dataController = DataController()
        router.get("/data/:dataId").handler(dataController::getData)
        router.post("/data/:dataId").handler{ dataController::postData }
        router.delete("/data/:dataId").handler{ dataController::deleteData }
        router.post("/data/find").handler{ dataController::findData }

        val scriptController = ScriptController()
        router.get("/script/:scriptId").handler(scriptController::getScript)
        router.post("/script/:scriptId").handler{ scriptController::postScript }
        router.delete("/script/:scriptId").handler{ scriptController::deleteScript }
        router.post("/script/find").handler{ scriptController::findScript }

        vertx.createHttpServer().requestHandler{ router.accept(it) }.listen(8080)
    }
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val vertx = Vertx.vertx()
            vertx.deployVerticle("io.cyber.hivemind.MainVerticle")
        }
    }

}

fun main(args : Array<String>) {
    MainVerticle.main(args)
}
