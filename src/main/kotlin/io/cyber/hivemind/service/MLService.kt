package io.cyber.hivemind.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import io.cyber.hivemind.RunState
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import java.util.*
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.ContainerCreation
import com.spotify.docker.client.messages.HostConfig
import com.spotify.docker.client.messages.PortBinding
import io.cyber.hivemind.Meta
import io.cyber.hivemind.MetaList
import io.cyber.hivemind.constant.*
import io.cyber.hivemind.model.RunConfig
import io.cyber.hivemind.util.dockerHostDir
import io.vertx.core.Future
import io.vertx.core.file.FileSystem
import org.apache.commons.lang.SystemUtils
import java.nio.file.Files
import java.nio.file.Paths
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.HashSet


interface MLService {
    fun prepareMachine(modelId: UUID)
    fun train(scriptId: UUID, modelId: UUID, dataId: UUID): Meta
    fun getRunConfig(scriptId: UUID): RunConfig
    fun runData(modelId: UUID, dataId: List<UUID>): Meta
    fun applyData(modelId: UUID, json: JsonObject, handler: Handler<AsyncResult<JsonObject>>)
    fun find(meta: Meta): MetaList
}

class MLServiceImpl(val vertx: Vertx) : MLService {

    val client = WebClient.create(vertx)
    val docker: DockerClient
    val fileSystem: FileSystem = vertx.fileSystem()
    val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory()).also { it.registerModule(KotlinModule()) }

    val tempState: MutableMap<UUID, Meta> = HashMap()
    val killedContainers: MutableSet<String> = HashSet()
    val isTraining = HashMap<UUID, AtomicBoolean>()

    init {
        docker = if (!SystemUtils.IS_OS_WINDOWS) {
            DefaultDockerClient(DOCKER_LOCAL_URI_UNIX)
        } else {
            DefaultDockerClient.fromEnv().build()
        }
    }


    override fun prepareMachine(modelId: UUID) {
        fileSystem.mkdirsBlocking("$LOCAL_MODEL$modelId/1")
    }

    override fun getRunConfig(scriptId: UUID): RunConfig {
        return Files.newBufferedReader(Paths.get("$LOCAL_SCRIPT$scriptId/$RUN_CONF_YML")).use {
            yamlMapper.readValue(it, RunConfig::class.java)
        }
    }

    override fun find(meta: Meta): MetaList {
        val res = tempState[meta.modelId]
        if (res != null) {
            return MetaList().also { it.add(res) }
        }
        val containers = docker.listContainers(DockerClient.ListContainersParam.withLabel(MODEL_ID, meta.modelId.toString()))
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

    //todo check no such file
    //todo check statuses
    override fun train(scriptId: UUID, modelId: UUID, dataId: UUID): Meta {
        Thread {
            var doTrain = true
            try {
                if (!isTraining.containsKey(modelId)) {
                    synchronized(isTraining) {
                        isTraining.putIfAbsent(modelId, AtomicBoolean())
                    }
                }
                doTrain = (isTraining[modelId]!!.compareAndSet(false, true))
                if (doTrain) {
                    println("training model $modelId from script $scriptId, with data $dataId")
                    tempState[modelId] = Meta(null, modelId, null, RunState.RUNNING, Date(), null)
                    removeContainers(modelId)
                    prepareMachine(modelId)
                    val runConfig = getRunConfig(scriptId)

                    val trainContainerId = trainInContainer(scriptId, modelId, dataId, runConfig)
                    docker.waitContainer(trainContainerId)
                    if (!killedContainers.contains(trainContainerId)) {
                        runServableContainer(modelId, runConfig.isPullImages())
                    }
                } else {
                    println("Can't start the training right now. Model $modelId is busy.")
                }
            } finally {
                if (doTrain) {
                    tempState.remove(modelId)
                    synchronized(isTraining) {
                        isTraining[modelId]?.set(false)
                    }
                }
            }
        }.start()
        return Meta(scriptId, modelId, dataId, RunState.RUNNING, null, null)
    }

    private fun trainInContainer(scriptId: UUID, modelId: UUID, dataId: UUID, runConfig: RunConfig): String {

        val labels = hashMapOf(
                SERVICE to TRAINING,
                MODEL_ID to modelId.toString()
        )

        if (runConfig.isPullImages()) {
            docker.pull(runConfig.image)
        }

        var binds = listOf(
                "$LOCAL_SCRIPT$scriptId${SEP}src".dockerHostDir() + ":/src:ro",
                "$LOCAL_DATA$dataId".dockerHostDir() + ":/data:ro",
                "$LOCAL_MODEL$modelId".dockerHostDir() + ":/tf_export")

        if (runConfig.isExportSession()) {
            binds += "$LOCAL_MODEL$modelId${SEP}tf_session".dockerHostDir() + ":/tf_session"
        }

        val hostConfBuilder = HostConfig.builder()
                .binds(binds)

        if (runConfig.getRuntime() != null) {
            hostConfBuilder.runtime(runConfig.getRuntime())
        }

        val hostConfig: HostConfig = hostConfBuilder.build()

        val containerConfig: ContainerConfig = ContainerConfig.builder().workingDir("$LOCAL_SCRIPT$scriptId".dockerHostDir())
                .image(runConfig.image)
                .labels(labels)
                .hostConfig(hostConfig)
                .cmd(runConfig.cmd)
                .build()
        val creation: ContainerCreation = docker.createContainer(containerConfig)
        val id = creation.id()
        docker.startContainer(id)
        return id!!
    }

    private fun runServableContainer(modelId: UUID, pullServingImage: Boolean): String {
        if (pullServingImage) {
            docker.pull(TF_SERVING)
        }
        val labels = hashMapOf(
                SERVICE to SERVING,
                MODEL_ID to modelId.toString()
        )

        val ports = arrayOf("8500", "8501")
        val portBindings = HashMap<String, List<PortBinding>>()
        for (port in ports) {
            val hostPorts = ArrayList<PortBinding>()
            hostPorts.add(PortBinding.of("0.0.0.0", port))
            portBindings["$port/tcp"] = hostPorts
        }

        val hostConfig: HostConfig =
                HostConfig.builder()
                        .binds("$LOCAL_MODEL$modelId".dockerHostDir() + ":/models/$modelId")
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
            container.labels()?.containsKey(MODEL_ID) ?: false &&
                    container.labels()?.get(MODEL_ID) == modelId.toString()
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

    companion object {
        const val TF_SERVING = "tensorflow/serving:latest"
        const val TF_SERVABlE_HOST = "localhost"
        const val TF_SERVABlE_PORT = 8501
        const val TF_SERVABlE_URI = "/v1/models"
    }

}