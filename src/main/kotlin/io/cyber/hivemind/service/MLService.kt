package io.cyber.hivemind.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.*
import io.cyber.hivemind.*
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import java.util.*
import io.cyber.hivemind.constant.*
import io.cyber.hivemind.model.RunConfig
import io.cyber.hivemind.util.dockerHostDir
import io.cyber.hivemind.util.toJson
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.file.FileSystem
import org.apache.commons.lang.SystemUtils
import java.nio.file.Files
import java.nio.file.Paths
import java.util.ArrayList
import java.util.HashMap
import kotlin.collections.HashSet


interface MLService {
    fun prepareMachine(modelId: UUID)
    fun train(scriptId: UUID, modelId: UUID, dataId: UUID, gpuSupported: Boolean): Meta
    fun getRunConfig(scriptId: UUID): RunConfig
    fun applyData(modelId: UUID, json: JsonObject, handler: Handler<AsyncResult<JsonObject>>)
    fun getModelsInTraining(stopped : Boolean = false): MetaList
    fun getModelsInServing(stopped : Boolean = false): MetaList
}

class MLServiceImpl(val vertx: Vertx) : MLService {

    val client = WebClient.create(vertx)
    val docker: DockerClient
    val fileSystem: FileSystem = vertx.fileSystem()
    val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory()).also { it.registerModule(KotlinModule()) }

    val killedContainers: MutableSet<String> = HashSet()

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

    private fun getMetaFromContainer(container: Container): Meta {
        val labels = container.labels()!!
        val state: RunState = when (labels[SERVICE]) {
            TRAINING -> RunState.TRAINING
            SERVING -> RunState.SERVING
            else -> RunState.ERROR
        }
        return Meta(labels[SCRIPT_ID], labels[MODEL_ID], labels[DATA_ID], state, Date(container.created()), null)
    }

    override fun getModelsInTraining(stopped: Boolean): MetaList {
        return getContainers(stopped, TRAINING)
    }

    override fun getModelsInServing(stopped: Boolean): MetaList {
        return getContainers(stopped, SERVING)
    }

    private fun getContainers(stopped: Boolean, label: String): MetaList {
        val withLabelParam = DockerClient.ListContainersParam.withLabel(SERVICE, label)
        if (!stopped) {
            return MetaList(docker.listContainers(withLabelParam).map { c -> getMetaFromContainer(c) })
        } else {
            val stoppedParam = DockerClient.ListContainersParam.withStatusExited()
            return MetaList(docker.listContainers(withLabelParam, stoppedParam).map { c -> getMetaFromContainer(c) })
        }
    }

    //todo check no such file
    //todo check statuses
    override fun train(scriptId: UUID, modelId: UUID, dataId: UUID, gpuSupported: Boolean): Meta {
        Thread {
            try {
                if (checkCanStartTraining(modelId)) {
                    println("training model $modelId from script $scriptId, with data $dataId")
                    removeContainers(modelId)
                    prepareMachine(modelId)
                    var runConfig = getRunConfig(scriptId)
                    if (!gpuSupported) {
                        runConfig = runConfig.copy(
                                image = runConfig.image.replace("latest-gpu-py3", "latest-py3"),
                                opt = HashMap(runConfig.opt).also { it.remove(RunConfig.RUNTIME) }
                        )
                    }
                    val trainContainerId = trainInContainer(scriptId, modelId, dataId, runConfig)
                    docker.waitContainer(trainContainerId)
                    if (!killedContainers.contains(trainContainerId)) {
                        runServableContainer(scriptId, modelId, dataId, runConfig.isPullImages())
                    }
                } else {
                    println("Can't start the training right now. Model $modelId is busy.")
                }
            } catch (t : Throwable) {
                print(t.message)
            }
        }.start()
        return Meta(scriptId, modelId, dataId, RunState.TRAINING, null, null)
    }

    @Synchronized
    private fun checkCanStartTraining(modelId: UUID): Boolean {
        return getModelsInTraining().filter { it.modelId == modelId }.isEmpty()
    }

    private fun trainInContainer(scriptId: UUID, modelId: UUID, dataId: UUID, runConfig: RunConfig): String {

        val labels = hashMapOf(
                SERVICE to TRAINING,
                MODEL_ID to modelId.toString(),
                SCRIPT_ID to scriptId.toString(),
                DATA_ID to dataId.toString()
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
        val meta = getModelsInTraining().filter { it.modelId == modelId }.first()
        notifyModelMetaUpdate(meta)
        return id!!
    }

    private fun notifyModelMetaUpdate(meta: Meta) {
        val cmd = Command(Type.MODEL, Verb.POST, Buffer.buffer(toJson(meta)))
        val opts = DeliveryOptions()
        opts.addHeader(ID, meta.modelId.toString())
        vertx.eventBus().send(FileVerticle::class.java.name, cmd, opts)
    }

    private fun runServableContainer(scriptId: UUID, modelId: UUID, dataId: UUID, pullServingImage: Boolean): String {
        if (pullServingImage) {
            docker.pull(TF_SERVING)
        }
        val labels = hashMapOf(
                SERVICE to SERVING,
                MODEL_ID to modelId.toString(),
                SCRIPT_ID to scriptId.toString(),
                DATA_ID to dataId.toString()
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
        val meta = getModelsInServing().filter { it.modelId == modelId }.first()
        notifyModelMetaUpdate(meta)
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

    override fun applyData(modelId: UUID, json: JsonObject, handler: Handler<AsyncResult<JsonObject>>) {
        //todo faster model lookup
        val modelMeta = getModelsInServing().filter { meta -> meta.modelId == modelId }
        if (!modelMeta.isEmpty() && RunState.SERVING == modelMeta[0].state) {
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
