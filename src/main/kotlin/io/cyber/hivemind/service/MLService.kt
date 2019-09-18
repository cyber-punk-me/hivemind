package io.cyber.hivemind.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
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
import io.vertx.core.eventbus.Message
import io.vertx.core.file.FileSystem
import org.apache.commons.lang.SystemUtils
import java.nio.file.Files
import java.nio.file.Paths
import java.util.ArrayList
import java.util.HashMap
import kotlin.collections.HashSet


interface MLService {
    fun train(scriptId: UUID, modelId: UUID, dataId: UUID, handler: Handler<AsyncResult<Meta>>)
    fun getRunConfig(scriptId: UUID): RunConfig
    fun applyData(modelId: UUID, json: JsonObject, handler: Handler<AsyncResult<JsonObject>>)
    fun getModelsInTraining(stopped: Boolean = false): MetaList
    fun getModelsInServing(stopped: Boolean = false): MetaList
}

private const val MODEL_ID = "modelId"
private const val DATA_ID = "dataId"
private const val SCRIPT_ID = "scriptId"
private const val SYSTEM_PROPERTY_PROFILE_NAME = "profile"
private const val ENV_VARIABLE_PROFILE_NAME = "HIVEMIND_PROFILE"
private const val DEFAULT_PROFILE = "cpu"
private const val TF_EXPORT = "/tf_export"
private const val TF_SESSION = "/tf_session"
private const val TF_SERVING = "tensorflow/serving:latest"
private const val TF_SERVABlE_HOST = "localhost"
private const val TF_SERVABlE_PORT = 8501
private const val TF_SERVABlE_URI = "/v1/models"

class MLServiceImpl(val vertx: Vertx) : MLService {

    val client = WebClient.create(vertx)
    val docker: DockerClient
    val fileSystem: FileSystem = vertx.fileSystem()
    val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory()).also { it.registerModule(KotlinModule()) }
    val profile: String = System.getProperty(SYSTEM_PROPERTY_PROFILE_NAME)
            ?: System.getenv(ENV_VARIABLE_PROFILE_NAME)
            ?: DEFAULT_PROFILE

    val killedContainers: MutableSet<String> = HashSet()

    init {
        docker = if (!SystemUtils.IS_OS_WINDOWS) {
            DefaultDockerClient(DOCKER_LOCAL_URI_UNIX)
        } else {
            DefaultDockerClient.fromEnv().build()
        }
    }


    private fun prepareLocalMachine(scriptId: UUID, modelId: UUID, dataId: UUID): Boolean {
        fileSystem.mkdirsBlocking("$LOCAL_MODEL$modelId/1")
        return fileSystem.existsBlocking("$LOCAL_SCRIPT$scriptId") &&
                fileSystem.existsBlocking("$LOCAL_DATA$dataId")
    }

    override fun getRunConfig(scriptId: UUID): RunConfig {
        val configsSource = Files.newBufferedReader(Paths.get("$LOCAL_SCRIPT$scriptId/$RUN_CONF_YML"))
        val runConfigsByName = yamlMapper.readValue<Map<String, RunConfig>>(configsSource)
        return runConfigsByName[profile]
                ?: throw RuntimeException("Profile '$profile' not found, available profiles are ${runConfigsByName.keys}")
    }

    private fun getMetaFromContainer(container: Container): Meta {
        val labels = container.labels()!!
        val state: RunState = when (labels[SERVICE]) {
            TRAINING -> RunState.TRAINING
            SERVING -> RunState.SERVING
            else -> RunState.ERROR
        }
        return Meta(labels[SCRIPT_ID], labels[MODEL_ID], labels[DATA_ID], state, Date(container.created()))
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

    override fun train(scriptId: UUID, modelId: UUID, dataId: UUID, handler: Handler<AsyncResult<Meta>>) {
        try {
            if (checkCanStartTraining(modelId)) {
                println("training model $modelId from script $scriptId, with data $dataId")
                removeContainers(modelId)

                removeModel(modelId, Handler { removeRes: AsyncResult<Message<Meta>> ->
                    if (removeRes.succeeded() && prepareLocalMachine(scriptId, modelId, dataId)) {
                        val runConfig = getRunConfig(scriptId)
                        Thread {
                            val trainContainerId = trainInContainer(scriptId, modelId, dataId, runConfig)
                            docker.waitContainer(trainContainerId)
                            if (!killedContainers.contains(trainContainerId)) {
                                runServableContainer(scriptId, modelId, dataId, runConfig.isPullImages())
                            }
                        }.start()
                        handler.handle(Future.succeededFuture(
                                Meta(scriptId, modelId, dataId, RunState.TRAINING)))
                    } else {
                        handler.handle(Future.succeededFuture(
                                Meta(scriptId, modelId, dataId, RunState.ERROR)))
                    }
                })
            } else {
                println("Can't start the training right now. Model $modelId is busy.")
                handler.handle(Future.succeededFuture(
                        Meta(scriptId, modelId, dataId, RunState.ERROR)))
            }
        } catch (t: Throwable) {
            print(t.message)
        }


    }

    @Synchronized
    private fun checkCanStartTraining(modelId: UUID): Boolean {
        return getModelsInTraining().none { it.modelId == modelId }
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

        val binds = mutableListOf(
                "$LOCAL_SCRIPT$scriptId${SEP}src".dockerHostDir() + ":/src:ro",
                "$LOCAL_DATA$dataId".dockerHostDir() + ":/data:ro",
                "$LOCAL_MODEL$modelId".dockerHostDir() + ":$TF_EXPORT"
        )


        if (runConfig.isExportSession()) {
            binds.add("$LOCAL_MODEL$modelId${SEP}tf_session".dockerHostDir() + ":$TF_SESSION")
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
                .cmd(getCMD(runConfig.cmd, runConfig.isExportSession()))
                .build()
        val creation: ContainerCreation = docker.createContainer(containerConfig)
        val id = creation.id()
        docker.startContainer(id)
        val meta = getModelsInTraining().first { it.modelId == modelId }
        notifyModelMetaUpdate(meta)
        return id!!
    }

    private fun getCMD(cmd: List<String>, exportSession : Boolean) : List<String> {
        val result = cmd.toMutableList()
        val iLast = cmd.size - 1
        //making container directories deletable by hivemind
        result[iLast] = "chmod -R go+w $TF_EXPORT && " + result[iLast] + " && chmod -R go+w $TF_EXPORT"

        if (exportSession) {
            result[iLast] = "chmod -R go+w $TF_SESSION && " + result[iLast] + " && chmod -R go+w $TF_SESSION"
        }
        return result
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
                        .binds(getModelExportVolumePath(modelId) + ":/models/$modelId:ro")
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
        val id = creation.id()!!
        //val info = docker.inspectContainer(id)
        docker.startContainer(id)
        val meta = getModelsInServing().first { it.modelId == modelId }
        notifyModelMetaUpdate(meta)
        return id
    }

    private fun getModelExportVolumePath(modelId: UUID) = "$LOCAL_MODEL$modelId".dockerHostDir()

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

    private fun removeModel(modelId: UUID, handler: Handler<AsyncResult<Message<Meta>>>) {
        val cmd = Command(Type.MODEL, Verb.DELETE)
        val opts = DeliveryOptions()
        opts.addHeader(ID, modelId.toString())
        vertx.eventBus().send(FileVerticle::class.java.name, cmd, opts) { ar: AsyncResult<Message<Meta>>? ->
            handler.handle(ar)
        }
    }

    override fun applyData(modelId: UUID, json: JsonObject, handler: Handler<AsyncResult<JsonObject>>) {
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

}
