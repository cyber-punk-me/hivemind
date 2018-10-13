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
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.ContainerCreation
import com.spotify.docker.client.messages.HostConfig
import com.spotify.docker.client.messages.PortBinding
import io.cyber.hivemind.util.unzipData
import io.vertx.core.file.FileSystem
import java.util.ArrayList
import java.util.HashMap




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

    //fileSystem.deleteRecursiveBlocking("$workDir/local/model/$modelId/1", true)
    //fileSystem.mkdirsBlocking("$workDir/local/model/$modelId/1")
    //fileSystem.copyRecursiveBlocking("$workDir/local/script/$scriptId/data/1", "$workDir/local/model/$modelId/1", true)
    override fun train(scriptId: UUID, modelId: UUID, dataId: UUID): RunStatus {
        print("training model $modelId from script $scriptId, with data $dataId")
        unzipData(File("$workDir/local/script/$scriptId"), "$workDir/local/script/$scriptId/script.zip")
        trainInContainer(scriptId, modelId, dataId, tfCpuPy3)
        //runServableContainer(scriptId, modelId, dataId)
        return RunStatus(RunState.RUNNING, Date(), null, scriptId, modelId, dataId)
    }

    private fun trainInContainer(scriptId: UUID, modelId: UUID, dataId: UUID, baseImage: String): RunStatus {
        val hostConfig: HostConfig =
                HostConfig.builder()
                        .binds("$workDir/local/script/$scriptId:/script-1/", "$workDir/local/data/$dataId:/data", "$workDir/local/model/$modelId:/model")
                        .build()
        docker.pull(baseImage)
        val containerConfig: ContainerConfig = ContainerConfig.builder().workingDir("$workDir/local/script/$scriptId")
                .image(baseImage)
                .hostConfig(hostConfig)
                .cmd("bash", "-c", "apt-get update && " +
                        "apt-get install -y python3-pip &&" +
                        "apt-get install -y git &&" +
                        "python3 -m pip install numpy sklearn myo-python &&" +
                        "cd /script-1 && ls && rm -r data/1 " +
                        "&& rm -r data/tensorflow_sessions " +
                        "&& python3 train.py " +
                        "&& cp -r data/1 /model")
                .build()
        val creation: ContainerCreation = docker.createContainer(containerConfig)
        val id = creation.id()
        //val info = docker.inspectContainer(id)
        docker.startContainer(id)
        return RunStatus(RunState.RUNNING, Date(), null, scriptId, modelId, dataId)
    }

    private fun runServableContainer(scriptId: UUID, modelId: UUID, dataId: UUID): RunStatus {
        docker.pull(tfServing)

        // Bind container ports to host ports
        val ports = arrayOf("8500", "8501")
        val portBindings = HashMap<String, List<PortBinding>>()
        for (port in ports) {
            val hostPorts = ArrayList<PortBinding>()
            hostPorts.add(PortBinding.of("0.0.0.0", port))
            portBindings["$port/tcp"] = hostPorts
        }

        val hostConfig: HostConfig =
                HostConfig.builder()
                        .binds("$workDir/local/model/$modelId:/models/$modelId")
                        .portBindings(portBindings)
                        .build()

        val containerConfig: ContainerConfig = ContainerConfig.builder()
                .image(tfServing)
                .exposedPorts(HashSet(ports.asList()))
                .env("MODEL_NAME=$modelId")
                .hostConfig(hostConfig)
                .build()

        val creation: ContainerCreation = docker.createContainer(containerConfig)
        val id = creation.id()
        //val info = docker.inspectContainer(id)
        docker.startContainer(id)
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
        const val tfNvidiaPy3 = "tensorflow/tensorflow:latest-gpu-py3"
        const val tfCpuPy3 = "tensorflow/tensorflow:latest-py3"
        const val tfServing = "tensorflow/serving:latest"
    }

}