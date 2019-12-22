package io.cyber.hivemind.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.InputStream

/**
 * User: kirillskiy
 * Date: 22/07/2018
 * Time: 14:47
 */

val mapper = ObjectMapper().registerKotlinModule().also {
    it.setSerializationInclusion(JsonInclude.Include.NON_NULL)
}


fun <T> fromJson(json: String, clazz : Class<T>) : T {
    return mapper.readValue(json, clazz)
}

fun <T> fromJson(json: InputStream, clazz : Class<T>) : T {
    return mapper.readValue(json, clazz)
}

fun toJson(obj : Any) : String {
    return mapper.writeValueAsString(obj)
}

