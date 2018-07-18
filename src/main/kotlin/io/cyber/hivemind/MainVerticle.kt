package io.cyber.hivemind

import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerRequest

class MainVerticle : AbstractVerticle() {

    override fun start() {
        vertx.createHttpServer()
                .requestHandler { req ->
                    println(req.uri())
                    when (req.uri()) {
                        "/data" -> doSet(req)
                        "/model" -> doModel(req)
                        "/apply" -> doModel(req)
                        "/script" -> doModel(req)
                        else -> {
                            req.response().end("Hello Vert.x!?")
                        }
                    }
                }.listen(8080)
    }
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val vertx = Vertx.vertx()
            vertx.deployVerticle("io.cyber.hivemind.MainVerticle")        }
    }

}

private fun doSet(req: HttpServerRequest?) {
}

private fun doModel(req: HttpServerRequest?) {
}

fun main(args : Array<String>) {
    MainVerticle.main(args)
}
