package io.cyber.hivemind.constant

import io.cyber.hivemind.model.ResourceType
import java.io.File

const val EXT = "ext"
const val SERVICE = "service"
const val TRAINING = "training"
const val SERVING = "serving"
const val RUN_CONF_YML = "runconf.yml"
const val ZIP_NAME = ".zip"


val WORK_DIR = System.getProperty("user.dir")!!
val SEP = File.separator!!
val LOCAL_ROOT = "$WORK_DIR${SEP}local$SEP"
val LOCAL_DATA = "${LOCAL_ROOT}data$SEP"
val LOCAL_MODEL = "${LOCAL_ROOT}model$SEP"
val LOCAL_SCRIPT = "${LOCAL_ROOT}script$SEP"
val HIVEMIND_FILE = ".hm"

const val DOCKER_LOCAL_URI_UNIX = "unix:///var/run/docker.sock"

fun getBaseDir(type: ResourceType): String {
    return when (type) {
        ResourceType.DATA -> LOCAL_DATA
        ResourceType.MODEL -> LOCAL_MODEL
        ResourceType.SCRIPT -> LOCAL_SCRIPT
    }
}

