package io.cyber.hivemind

import io.vertx.ext.web.RoutingContext

class DataController {
    fun getData(routingContext: RoutingContext) {
        val dataId = routingContext.request().getParam("dataId")
    }

    fun postData(context: RoutingContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun deleteData(context: RoutingContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun findData(context: RoutingContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
