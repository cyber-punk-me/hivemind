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
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.ContainerCreation
import com.spotify.docker.client.messages.HostConfig
import io.cyber.hivemind.util.unzipData
import io.vertx.core.file.FileSystem


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
    val docker: DockerClient = DefaultDockerClient("unix:///var/run/docker.sock")
    val fileSystem: FileSystem = vertx.fileSystem()

    override fun train(scriptId: UUID, modelId: UUID, dataId: UUID): RunStatus {
        print("training model $modelId from script $scriptId, with data $dataId")
        //trainInContainer(scriptId, modelId, dataId, tfCpuPy3)
        fileSystem.mkdirsBlocking("$workDir/local/model/$modelId/1")
        fileSystem.copyRecursive("$workDir/local/script/$scriptId/data/1", "$workDir/local/model/$modelId/1", true) {
            runServableContainer(scriptId, modelId, dataId)
        }
        return RunStatus(RunState.RUNNING, Date(), null, scriptId, modelId, dataId)
    }

    private fun trainInContainer(scriptId: UUID, modelId: UUID, dataId: UUID, baseImage: String): RunStatus {
        unzipData(File("$workDir/local/script/$scriptId"), "$workDir/local/script/$scriptId/script.zip")
        val hostConfig: HostConfig =
                HostConfig.builder()
                        .binds("$workDir/local/script/$scriptId:/script-1/")
                        .build()
        docker.pull(baseImage)
        val containerConfig: ContainerConfig = ContainerConfig.builder().workingDir("$workDir/local/script/$scriptId")
                .image(baseImage)
                .hostConfig(hostConfig)
                .cmd("bash", "-c", "apt-get update && " +
                        "apt-get install -y python3-pip &&" +
                        "apt-get install -y git &&" +
                        "python3 -m pip install numpy sklearn myo-python &&" +
                        "cd /script-1 && ls && python3 train.py")
                .build()
        val creation: ContainerCreation = docker.createContainer(containerConfig)
        val id = creation.id()
        //val info = docker.inspectContainer(id)
        docker.startContainer(id)
        return RunStatus(RunState.RUNNING, Date(), null, scriptId, modelId, dataId)
    }

    private fun runServableContainer(scriptId: UUID, modelId: UUID, dataId: UUID): RunStatus {
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
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }


    companion object {
        val workDir = System.getProperty("user.dir")
        val tfNvidiaPy3 = "tensorflow/tensorflow:latest-gpu-py3"
        val tfCpuPy3 = "tensorflow/tensorflow:latest-py3"
    }

}