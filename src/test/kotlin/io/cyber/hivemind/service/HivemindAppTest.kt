package io.cyber.hivemind.service

import io.cyber.hivemind.main
import io.ktor.http.*
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.junit.Test
import java.nio.file.Files
import kotlin.test.*


class HivemindAppTest {

    @Test
    fun testTrainModel() = testApp {
        handleRequest(HttpMethod.Post, "http://localhost:8080/model/1d722019-c892-44bc-844b-eb5708d55987") {
            this.setBody("{\"scriptId\" = \"7de76908-d4d9-4ce9-98de-118a4fb3b8f8\", \"dataId\" = \"5d335160-bd2a-45e4-9199-8105a38941ad\"}")
        }.apply {
            assertEquals(200, response.status()?.value)
        }
    }
}


private fun testApp(callback: TestApplicationEngine.() -> Unit) {
    val tempPath = Files.createTempDirectory(null).toFile().apply { deleteOnExit() }
    try {
        withTestApplication({
            //install(ContentNegotiation)
            main()
        }, callback)
    } finally {
        tempPath.deleteRecursively()
    }
}

