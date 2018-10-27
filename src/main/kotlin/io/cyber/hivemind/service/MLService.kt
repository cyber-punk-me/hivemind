package io.cyber.hivemind.service

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import io.cyber.hivemind.RunState
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
import io.cyber.hivemind.Meta
import io.cyber.hivemind.MetaList
import io.vertx.core.Future
import io.vertx.core.file.FileSystem
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.HashSet


interface MLService {
    fun train(scriptId: UUID, modelId: UUID, dataId: UUID, gpuTrain: Boolean): Meta
    fun runData(modelId: UUID, dataId: List<UUID>): Meta
    fun applyData(modelId: UUID, json: JsonObject, handler: Handler<AsyncResult<JsonObject>>)
    fun find(meta: Meta): MetaList
}

class MLServiceImpl(val vertx: Vertx) : MLService {

    val client = WebClient.create(vertx)
    val docker: DockerClient = DefaultDockerClient("unix:///var/run/docker.sock")
    val fileSystem: FileSystem = vertx.fileSystem()
    val tempState: MutableMap<UUID, Meta> = HashMap()
    val killedContainers: MutableSet<String> = HashSet()
    val isTraining = HashMap<UUID, AtomicBoolean>()

    override fun find(meta: Meta): MetaList {
        val res = tempState[meta.modelId]
        if (res != null) {
            return MetaList().also { it.add(res) }
        }
        val containers = docker.listContainers(DockerClient.ListContainersParam.withLabel("modelId", meta.modelId.toString()))
        if (containers.isEmpty()) {
            return MetaList()
        }
        val container = containers.first()
        return if (container.labels()!!["service"].equals("training")) {
            MetaList().also { it.add(Meta(null, meta.modelId, null, RunState.RUNNING, null, null)) }
        } else {
            MetaList().also { it.add(Meta(null, meta.modelId, null, RunState.COMPLETE, null, null)) }
        }
    }

    override fun train(scriptId: UUID, modelId: UUID, dataId: UUID, gpuTrain: Boolean): Meta {
        Thread {
            var doTrain = true
            try {
                if (MODEL_TRAINING_LOCK) {
                    if (!isTraining.containsKey(modelId)) {
                        synchronized(isTraining) {
                            isTraining.putIfAbsent(modelId, AtomicBoolean())
                        }
                    }
                    doTrain = (isTraining[modelId]!!.compareAndSet(false, true))
                }
                if (doTrain) {
                    println("training model $modelId from script $scriptId, with data $dataId")
                    tempState[modelId] = Meta(null, modelId, null, RunState.RUNNING, Date(), null)
                    removeContainers(modelId)
                    val trainContainerId = trainInContainer(scriptId, modelId, dataId, gpuTrain)
                    docker.waitContainer(trainContainerId)
                    if (!killedContainers.contains(trainContainerId)) {
                        runServableContainer(scriptId, modelId, dataId)
                    }
                } else {
                    println("Can't start the training right now. Model $modelId is busy.")
                }
            } finally {
                if (doTrain) {
                    tempState.remove(modelId)
                    if (MODEL_TRAINING_LOCK) {
                        synchronized(isTraining) {
                            isTraining[modelId]?.set(false)
                        }
                    }
                }
            }
        }.start()
        return Meta(scriptId, modelId, dataId, RunState.RUNNING, null, null)
    }

    private fun trainInContainer(scriptId: UUID, modelId: UUID, dataId: UUID, nvidiaRuntime: Boolean): String {
        fileSystem.mkdirsBlocking("$workDir/local/model/$modelId/1")
        fileSystem.mkdirsBlocking("$workDir/local/tf_session/$modelId")

        val labels = hashMapOf(
                "service" to "training",
                "modelId" to modelId.toString()
        )

        val baseImage: String = if (nvidiaRuntime) TF_NVIDIA_PY3 else TF_CPU_PY3
        val hostConfBuilder = HostConfig.builder()
                .binds("$workDir/local/script/$scriptId/src:/src",
                        "$workDir/local/data/$dataId:/data",
                        "$workDir/local/tf_session/$modelId:/tf_session",
                        "$workDir/local/model/$modelId:/tf_export")

        if (nvidiaRuntime) {
            hostConfBuilder.runtime(NVIDIA_RUNTIME)
        }

        val hostConfig: HostConfig = hostConfBuilder.build()

        docker.pull(baseImage)
        val containerConfig: ContainerConfig = ContainerConfig.builder().workingDir("$workDir/local/script/$scriptId")
                .image(baseImage)
                .labels(labels)
                .hostConfig(hostConfig)
                .cmd("bash", "-c", "apt-get update && " +
                        "apt-get install -y python3-pip &&" +
                        "apt-get install -y git &&" +
                        "python3 -m pip install numpy sklearn myo-python &&" +
                        "cd /src && ls && " +
                        "python3 train.py")
                .build()
        val creation: ContainerCreation = docker.createContainer(containerConfig)
        val id = creation.id()
        docker.startContainer(id)
        return id!!
    }

    private fun runServableContainer(scriptId: UUID, modelId: UUID, dataId: UUID): String {

        val labels = hashMapOf(
                "service" to "serving",
                "modelId" to modelId.toString()
        )

        docker.pull(TF_SERVING)

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
                .image(TF_SERVING)
                .labels(labels)
                .exposedPorts(HashSet(ports.asList()))
                .env("MODEL_NAME=$modelId")
                .hostConfig(hostConfig)
                .build()

        val creation: ContainerCreation = docker.createContainer(containerConfig)
        val id = creation.id()
        //val info = docker.inspectContainer(id)
        docker.startContainer(id)
        return id!!
    }

    private fun removeContainers(modelId: UUID) {
        val containers = docker.listContainers(DockerClient.ListContainersParam.withStatusRunning())
        containers.filter { container ->
            container.labels()?.containsKey("modelId") ?: false &&
                    container.labels()?.get("modelId") == modelId.toString()
        }.forEach { container ->
            killedContainers.add(container.id())
            docker.killContainer(container.id())
            docker.removeContainer(container.id())
        }
    }

    override fun runData(modelId: UUID, dataId: List<UUID>): Meta {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun applyData(modelId: UUID, json: JsonObject, handler: Handler<AsyncResult<JsonObject>>) {
        val modelMeta = find(Meta(null, modelId, null, null, null, null))
        if (!modelMeta.isEmpty() && RunState.COMPLETE == modelMeta[0].state) {
            client.post(TF_SERVABlE_PORT, TF_SERVABlE_HOST, "$TF_SERVABlE_URI/$modelId:predict")
                    .sendJsonObject(json) { ar ->
                        run {
                            handler.handle(ar.map { http -> http.bodyAsJsonObject() })
                        }
                    }
        } else {
            handler.handle(Future.failedFuture("no servable"))
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
        const val NVIDIA_RUNTIME = "nvidia"
        const val TF_NVIDIA_PY3 = "tensorflow/tensorflow:latest-gpu-py3"
        const val TF_CPU_PY3 = "tensorflow/tensorflow:latest-py3"
        const val TF_SERVING = "tensorflow/serving:latest"
        const val TF_SERVABlE_HOST = "localhost"
        const val TF_SERVABlE_PORT = 8501
        const val TF_SERVABlE_URI = "/v1/models"
        const val MODEL_TRAINING_LOCK = true
    }

}