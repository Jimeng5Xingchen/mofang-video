package com.mofangvideo.core.update

/**
 * 更新渠道：正式版仅匹配正式 Release，Beta 额外包含预发布版本。
 */
enum class UpdateChannel {
    STABLE,
    BETA;

    companion object {
        fun fromPrefs(value: String?): UpdateChannel =
            if (value == BETA.name) BETA else STABLE
    }
}

/**
 * 可安装的新版本信息（已选定与设备形态匹配的 APK 资产）。
 */
data class AppUpdate(
    val versionName: String,
    val isPrerelease: Boolean,
    val releaseNotes: String,
    val apkUrl: String,
    val apkName: String,
    val apkSizeBytes: Long
)

/**
 * GitHub Release 原始信息（解析层，与设备无关）。
 */
data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val isPrerelease: Boolean,
    val body: String,
    val assets: List<ReleaseAsset>
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long
)

/**
 * 下载状态机。
 */
sealed class DownloadState {
    data class Progress(val downloadedBytes: Long, val totalBytes: Long) : DownloadState() {
        val percent: Int
            get() = if (totalBytes > 0) (downloadedBytes * 100 / totalBytes).toInt().coerceIn(0, 100) else 0
    }

    data class Done(val file: java.io.File) : DownloadState()
    data class Failed(val message: String) : DownloadState()
}
