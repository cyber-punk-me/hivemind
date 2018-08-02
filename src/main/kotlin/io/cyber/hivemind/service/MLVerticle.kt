package io.cyber.hivemind.service

import io.vertx.core.AbstractVerticle
import io.vertx.core.Context
import io.vertx.core.Vertx

class MLVerticle : AbstractVerticle() {

    var mLService: MLService? = null

    override fun init(vertx: Vertx, context: Context) {
        super.init(vertx, context)
        mLService = MLServiceImpl::class.java.newInstance()
    }
}