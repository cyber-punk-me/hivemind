package io.cyber.hivemind.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

/**
 * User: kirillskiy
 * Date: 22/07/2018
 * Time: 14:47
 */
val mapper = ObjectMapper().registerKotlinModule()


fun <T> fromJson(json: String, clazz : Class<T>) : T {
    return mapper.readValue(json, clazz)
}

fun toJson(obj : Any) : String {
    return mapper.writeValueAsString(obj)
}

