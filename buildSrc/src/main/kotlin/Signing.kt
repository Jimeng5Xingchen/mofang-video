/**
 * 签名配置解析：从 Gradle 属性或环境变量读取签名信息。
 *
 * 优先级：Gradle 属性（-Pxxx=） > 环境变量。
 * CI 中通过 `-P` 或环境变量注入，本地开发可放在 `local.properties`（已 gitignore）。
 */
object Signing {

    private fun prop(name: String, env: String): String? =
        (System.getProperty(name) ?: System.getenv(env)).orEmpty().ifBlank { null }

    /** 密钥库文件路径。 */
    val storeFile: String? get() = prop("RELEASE_STORE_FILE", "RELEASE_STORE_FILE")

    /** 密钥库密码。 */
    val storePassword: String? get() = prop("RELEASE_STORE_PASSWORD", "RELEASE_STORE_PASSWORD")

    /** 密钥别名。 */
    val keyAlias: String? get() = prop("RELEASE_KEY_ALIAS", "RELEASE_KEY_ALIAS")

    /** 密钥密码。 */
    val keyPassword: String? get() = prop("RELEASE_KEY_PASSWORD", "RELEASE_KEY_PASSWORD")

    /** 四项齐备且密钥库文件真实存在时才启用签名。 */
    val isConfigured: Boolean
        get() = storeFile != null &&
            storePassword != null &&
            keyAlias != null &&
            keyPassword != null &&
            java.io.File(storeFile!!).isFile
}
