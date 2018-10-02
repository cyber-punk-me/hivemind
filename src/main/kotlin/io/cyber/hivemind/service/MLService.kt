package io.cyber.hivemind.service

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import io.cyber.hivemind.RunState
import io.cyber.hivemind.RunStatus
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import com.spotify.docker.client.DockerClient.ListContainersParam



interface MLService {
    fun train(scriptId: UUID, modelId: UUID, dataId: UUID): RunStatus
    fun runData(modelId: UUID, dataId: List<UUID>): RunStatus
    fun applyData(modelId: UUID, json: JsonObject, handler: Handler<AsyncResult<JsonObject>>)
}

class MLServiceImpl(val vertx: Vertx) : MLService {

    val TF_SERVABlE_HOST = "localhost"
    val TF_SERVABlE_PORT = 8501
    val TF_SERVABlE_URI = "/v1/models/half_plus_three:predict"
    val client = WebClient.create(vertx)
    val docker : DockerClient = DefaultDockerClient("unix:///var/run/docker.sock")

    override fun train(scriptId: UUID, modelId: UUID, dataId: UUID): RunStatus {
        print("training model $modelId from script $scriptId, with data $dataId")
        //todo build
        val containers = docker.listContainers(ListContainersParam.allContainers())
        val container = containers.first { c -> c.names().contains("/model-1-servable") }
        docker.startContainer(container.id())
        return RunStatus(RunState.RUNNING, Date(), null, scriptId, modelId, dataId)
    }

    override fun runData(modelId: UUID, dataId: List<UUID>): RunStatus {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun applyData(modelId: UUID, json: JsonObject, handler: Handler<AsyncResult<JsonObject>>) {
        client.post(TF_SERVABlE_PORT, TF_SERVABlE_HOST, TF_SERVABlE_URI)
                .sendJsonObject(json) { ar ->
                    run {
                        handler.handle(ar.map { http -> http.bodyAsJsonObject() })
                    }
                }
    }

    private fun String.runCommand(workingDir: File): BufferedReader? {
        try {
            val parts = this.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                    .directory(workingDir)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()

            proc.waitFor(10, TimeUnit.MINUTES)
            return proc.inputStream.bufferedReader()
        } catch(e: IOException) {
            e.printStackTrace()
            return null
        }
    }

}