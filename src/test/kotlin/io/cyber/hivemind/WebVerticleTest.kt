package io.cyber.hivemind

import io.cyber.hivemind.service.FileVerticle
import io.vertx.core.Vertx
import io.vertx.core.file.OpenOptions
import io.vertx.core.streams.Pump
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import junit.framework.Assert.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files
import java.nio.file.Paths


/**
 * User: kirillskiy
 * Date: 28/07/2018
 * Time: 00:28
 */
@RunWith(VertxUnitRunner::class)
class WebVerticleTest {

    private val vertx: Vertx = Vertx.vertx()

    @Before
    fun setUp(tc: TestContext) {
        vertx.deployVerticle(WebVerticle::class.java.name, tc.asyncAssertSuccess())
        vertx.deployVerticle(FileVerticle::class.java.name, tc.asyncAssertSuccess())
    }

    @After
    fun tearDown(tc: TestContext) {
        vertx.close(tc.asyncAssertSuccess())
    }

    @Test
    fun testDownloadData(tc: TestContext) {
        val async = tc.async()
        vertx.createHttpClient().getNow(8080, "localhost", "/data/1") { response ->
            tc.assertEquals(response.statusCode(), 200)
            response.bodyHandler { body ->
                tc.assertTrue(body.length() > 0)
                async.complete()
            }
        }
    }

    @Test
    fun testUploadData(tc: TestContext) {
        val async = tc.async()
        val req = vertx.createHttpClient().put(8080, "localhost", "/data/1") { response -> println("File uploaded " + response.statusCode()) }

        val filename = "test.zip"

        // For a chunked upload you don't need to specify size, just do:
        //req.isChunked = true

        val openOptions = OpenOptions()
        openOptions.isWrite = true
        vertx.fileSystem().open(filename, openOptions) { ar ->
            val file = ar.result()
            // For a non-chunked upload you need to specify size of upload up-front
            req.headers().set("Content-Length", Files.size(Paths.get(filename)).toString())
            val pump = Pump.pump(file, req)
            pump.start()

            file.endHandler {
                file.close {
                    if (ar.succeeded()) {
                        req.end()
                        println("Sent request")
                    } else {
                        fail(System.err.toString())
                    }
                    async.complete()
                }
            }
        }

    }
}