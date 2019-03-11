package io.cyber.hivemind.constant

import io.cyber.hivemind.Type
import java.io.File

const val ID = "id"
const val EXT = "ext"
const val META = "meta"
const val MODEL_ID = "modelId"
const val DATA_ID = "dataId"
const val SCRIPT_ID = "scriptId"
const val SERVICE = "service"
const val TRAINING = "training"
const val SERVING = "serving"
const val RUN_CONF_YML = "runconf.yml"

val WORK_DIR = System.getProperty("user.dir")!!
val SEP = File.separator!!
val LOCAL_ROOT = "$WORK_DIR${SEP}local$SEP"
val LOCAL_DATA = "${LOCAL_ROOT}data$SEP"
val LOCAL_MODEL = "${LOCAL_ROOT}model$SEP"
val LOCAL_SCRIPT = "${LOCAL_ROOT}script$SEP"
val HIVEMIND_DIR = "$SEP.hm$SEP"
val META_LOCATION = "$HIVEMIND_DIR$META"

const val DOCKER_LOCAL_URI_UNIX = "unix:///var/run/docker.sock"

fun getBaseDir(type: Type): String {
    return when (type) {
        Type.DATA -> LOCAL_DATA
        Type.MODEL -> LOCAL_MODEL
        Type.SCRIPT -> LOCAL_SCRIPT
    }
}

