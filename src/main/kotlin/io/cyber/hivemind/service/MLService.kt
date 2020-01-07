package io.cyber.hivemind.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.*
import java.util.*
import io.cyber.hivemind.constant.*
import io.cyber.hivemind.model.ModelMeta
import io.cyber.hivemind.model.ResourceType
import io.cyber.hivemind.model.RunConfig
import io.cyber.hivemind.model.RunState
import io.cyber.hivemind.util.dockerHostDir
import org.apache.commons.lang.SystemUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.ArrayList
import java.util.HashMap
import kotlin.collections.HashSet


interface MLService {
    suspend fun train(scriptId: UUID, modelId: UUID, dataId: UUID): ModelMeta
    fun getRunConfig(scriptId: UUID): RunConfig
    suspend fun applyData(modelId: UUID, json: JsonNode): JsonNode
    fun getModelsInTraining(stopped: Boolean = false): List<ModelMeta>
    fun getModelsInServing(stopped: Boolean = false): List<ModelMeta>
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

class MLServiceImpl(val fileService: FileService) : MLService {

    private val logger: Logger = LoggerFactory.getLogger("FileUtil")
    private val restClient = RestClient(host = TF_SERVABlE_HOST, port = TF_SERVABlE_PORT)
    private val docker: DockerClient = if (!SystemUtils.IS_OS_WINDOWS) {
        DefaultDockerClient(DOCKER_LOCAL_URI_UNIX)
    } else {
        DefaultDockerClient.fromEnv().build()
    }
    private val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory()).also { it.registerModule(KotlinModule()) }
    private val profile: String = System.getProperty(SYSTEM_PROPERTY_PROFILE_NAME)
            ?: System.getenv(ENV_VARIABLE_PROFILE_NAME)
            ?: DEFAULT_PROFILE


    private fun prepareForLearning(scriptId: UUID, modelId: UUID, dataId: UUID) {
        val scriptExists = File("$LOCAL_SCRIPT$scriptId").exists()
        val dataDir = File("$LOCAL_DATA$dataId")
        val dataExists = dataDir.exists()
        val dataFilesExist = !(dataDir.listFiles()?.isEmpty() ?: true)
        val cleanedModelDir = fileService.delete(ResourceType.MODEL, modelId)
        val preparedModelDir = File("$LOCAL_MODEL$modelId/1").mkdirs()

        val goodToGo = scriptExists && dataExists && dataFilesExist && cleanedModelDir && preparedModelDir
        if (!goodToGo) {
            throw RuntimeException("Prepare for learning failed : scriptExists:$scriptExists, dataExists:$dataExists," +
                    " dataFilesExist:$dataFilesExist, cleanedModelDir:$cleanedModelDir, preparedModelDir:$preparedModelDir")
        }
    }

    override fun getRunConfig(scriptId: UUID): RunConfig {
        val configsSource = Files.newBufferedReader(Paths.get("$LOCAL_SCRIPT$scriptId/$RUN_CONF_YML"))
        val runConfigsByName = yamlMapper.readValue<Map<String, RunConfig>>(configsSource)
        return runConfigsByName[profile]
                ?: throw RuntimeException("Profile '$profile' not found, available profiles are ${runConfigsByName.keys}")
    }

    private fun getMetaFromContainer(container: Container): ModelMeta {
        val labels = container.labels()!!
        val state: RunState = when (labels[SERVICE]) {
            TRAINING -> RunState.TRAINING
            SERVING -> RunState.SERVING
            else -> RunState.ERROR
        }
        return ModelMeta(labels[SCRIPT_ID]!!, labels[MODEL_ID]!!, labels[DATA_ID]!!, state, container.created())
    }

    override fun getModelsInTraining(stopped: Boolean): List<ModelMeta> {
        return getContainers(stopped, TRAINING)
    }

    override fun getModelsInServing(stopped: Boolean): List<ModelMeta> {
        return getContainers(stopped, SERVING)
    }

    private fun getContainers(stopped: Boolean, label: String): List<ModelMeta> {
        val withLabelParam = DockerClient.ListContainersParam.withLabel(SERVICE, label)
        return if (!stopped) {
            docker.listContainers(withLabelParam).map { c -> getMetaFromContainer(c) }
        } else {
            val stoppedParam = DockerClient.ListContainersParam.withStatusExited()
            docker.listContainers(withLabelParam, stoppedParam).map { c -> getMetaFromContainer(c) }
        }
    }

    override suspend fun train(scriptId: UUID, modelId: UUID, dataId: UUID): ModelMeta {
        return try {
            if (getModelsInTraining().none { it.modelId == modelId }) {
                logger.info("training model $modelId from script $scriptId, with data $dataId")
                removeContainers(modelId)
                prepareForLearning(scriptId, modelId, dataId)
                val runConfig = getRunConfig(scriptId)
                Thread {
                    val trainContainerId = trainInContainer(scriptId, modelId, dataId, runConfig)
                    val exit = docker.waitContainer(trainContainerId)
                    val trainLog = trainContainerId.getDockerStdLog()
                    trainLog?.let { logger.info("Container $trainContainerId log: \n $it") }
                    if (exit.statusCode() == 0L) {
                        runServableContainer(scriptId, modelId, dataId, runConfig.isPullImages())
                    } else {
                        val trainError = trainContainerId.getDockerErrLog()
                        trainError?.let { logger.info("Container $trainContainerId error: \n $it") }
                    }
                }.start()
                ModelMeta(scriptId, modelId, dataId, RunState.TRAINING, System.currentTimeMillis())
            } else {
                logger.warn("Can't start the training right now. Model $modelId is busy.")
                ModelMeta(scriptId, modelId, dataId, RunState.ERROR, System.currentTimeMillis())
            }
        } catch (t: Throwable) {
            logger.error(t.message, t)
            ModelMeta(scriptId, modelId, dataId, RunState.ERROR, System.currentTimeMillis())
        }
    }

    private fun String.getDockerStdLog() : String? =
            docker.logs(this, DockerClient.LogsParam.stdout(), DockerClient.LogsParam.tail(50))?.readFully()

    private fun String.getDockerErrLog() : String? =
            docker.logs(this, DockerClient.LogsParam.stderr(), DockerClient.LogsParam.tail(50))?.readFully()

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
        fileService.notifyModelMetaUpdate(meta)
        return id!!
    }

    private fun getCMD(cmd: List<String>, exportSession: Boolean): List<String> {
        val result = cmd.toMutableList()
        val iLast = cmd.size - 1
        //making container directories deletable by hivemind
        result[iLast] = "chmod -R go+w $TF_EXPORT && " + result[iLast] + " && chmod -R go+w $TF_EXPORT"

        if (exportSession) {
            result[iLast] = "chmod -R go+w $TF_SESSION && " + result[iLast] + " && chmod -R go+w $TF_SESSION"
        }
        return result
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
        fileService.notifyModelMetaUpdate(meta)
        return id
    }

    private fun getModelExportVolumePath(modelId: UUID) = "$LOCAL_MODEL$modelId".dockerHostDir()

    private fun removeContainers(modelId: UUID) {
        val containers = docker.listContainers(DockerClient.ListContainersParam.withStatusRunning())
        containers.filter { container ->
            container.labels()?.containsKey(MODEL_ID) ?: false &&
                    container.labels()?.get(MODEL_ID) == modelId.toString()
        }.forEach { container ->
            docker.killContainer(container.id())
            docker.removeContainer(container.id())
        }
    }

    override suspend fun applyData(modelId: UUID, json: JsonNode): JsonNode {
        val modelMeta = getModelsInServing().filter { meta -> meta.modelId == modelId }
        if (!modelMeta.isEmpty() && RunState.SERVING == modelMeta[0].state) {
            return restClient.applyData("$TF_SERVABlE_URI/$modelId:predict", json)
        } else {
            throw IllegalArgumentException("Can't applyData to this model")
        }
    }

}
