package io.cyber.hivemind.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.cyber.hivemind.util.toJson
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent

class RestClient(val host: String, val port: Int) {

    suspend fun applyData(path: String, data: JsonNode): JsonNode =
            httpClient.post<ObjectNode>(scheme = "http", host = host, port = port, path = path,
                    body = TextContent(toJson(data), contentType = ContentType.Application.Json))

    companion object {
        private val httpClient = HttpClient(Apache) {
            install(JsonFeature)
        }
    }

}