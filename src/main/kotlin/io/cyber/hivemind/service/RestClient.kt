package io.cyber.hivemind.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.cyber.hivemind.util.fromJson
import io.cyber.hivemind.util.toJson
import io.ktor.client.*
import io.ktor.client.engine.apache.*
//import io.ktor.client.features.json.JsonFeature todo import error
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent

class RestClient(val host: String, val port: Int) {

    suspend fun applyData(path: String, data: JsonNode): JsonNode {
        val mlResponse = httpClient.post<String>(scheme = "http", host = host, port = port, path = path,
                body = TextContent(toJson(data), contentType = ContentType.Application.Json))
        return fromJson(mlResponse, ObjectNode::class.java)
    }

    companion object {
        private val httpClient = HttpClient(Apache) {
            //install(JsonFeature) todo import error
        }
    }
}