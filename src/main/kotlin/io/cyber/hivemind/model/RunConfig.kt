package io.cyber.hivemind.model

data class RunConfig(
    val cmd: List<String>,
    val image: String,
    val opt: Map<String, String>? = null,
    val devices: List<String>? = null
) {

    fun isExportSession() = getBoolOption(EXPORT_SESSION)
    fun isPullImages() = getBoolOption(PULL_IMAGES)
    fun getRuntime() = opt?.get(RUNTIME)

    private fun getBoolOption(key: String): Boolean = opt?.getOrDefault(key, FALSE)?.toBoolean() ?: false

    companion object {
        const val FALSE = "false"
        const val RUNTIME = "runtime"
        const val EXPORT_SESSION = "exportSession"
        const val PULL_IMAGES = "pullImages"
    }
}
