package io.cyber.hivemind

import io.vertx.ext.web.RoutingContext


/**
 * User: kirillskiy
 * Date: 22/07/2018
 * Time: 01:20
 */
class ModelController {
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}