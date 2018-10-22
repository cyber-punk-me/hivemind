package io.cyber.hivemind.util

import io.vertx.core.eventbus.DeliveryOptions

fun DeliveryOptions.addNotNullHeader(key : String, value : String?) {
    if (value != null) {
        addHeader(key, value)
    }
}