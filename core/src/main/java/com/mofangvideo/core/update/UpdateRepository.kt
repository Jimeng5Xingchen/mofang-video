package com.mofangvideo.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import com.mofangvideo.core.network.Net
import com.mofangvideo.core.repo.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Request
import org.json.JSONException
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * 应用内检查更新仓库：GitHub Releases 查询 + 安装包流式下载 + 安装引导。
 *
 * 设备形态按 FEATURE_LEANBACK 判定：TV 取 `mofang-video-tv-*` 资产，
 * 手机取 `mofang-video-phone-*` 资产。
 */
interface UpdateRepository {

    /**
     * 检查新版本。
     * @return [Result.Ok] 且值为 null 表示已是最新。
     */
    suspend fun checkUpdate(channel: UpdateChannel, currentVersion: String): Result<AppUpdate?>

    /** 流式下载目标 APK；收集方取消即中断并清理临时文件。 */
    fun downloadApk(update: AppUpdate): Flow<DownloadState>
}

class UpdateRepositoryImpl @Inject constructor(
    private val context: Context
) : UpdateRepository {

    private val assetPrefix: String by lazy {
        val isTv = context.packageManager
            .hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        if (isTv) TV_ASSET_PREFIX else PHONE_ASSET_PREFIX
    }

    override suspend fun checkUpdate(
        channel: UpdateChannel,
        currentVersion: String
    ): Result<AppUpdate?> {
        val client = Net.client.newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(RELEASES_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "mofang-video-app")
            .build()
        val body = try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return Result.Err(if (resp.code == 403) "请求过于频繁，请稍后再试" else "检查失败（HTTP ${resp.code}）")
                }
                resp.body?.string() ?: return Result.Err("响应为空")
            }
        } catch (e: IOException) {
            return Result.Err("网络错误：${e.message ?: "连接失败"}")
        }
        val releases = try {
            ReleaseParser.parseReleases(body)
        } catch (e: JSONException) {
            return Result.Err("版本信息解析失败")
        }
        return Result.Ok(ReleaseParser.pickUpdate(releases, channel, currentVersion, assetPrefix))
    }

    override fun downloadApk(update: AppUpdate): Flow<DownloadState> = flow {
        val dir = context.getExternalFilesDir(UPDATE_DIR)
            ?: File(context.filesDir, UPDATE_DIR).apply { mkdirs() }
        dir.mkdirs()
        val target = File(dir, APK_FILE)
        val tmp = File(dir, "$APK_FILE.tmp")
        target.delete()
        tmp.delete()
        try {
            val request = Request.Builder()
                .url(update.apkUrl)
                .header("User-Agent", "mofang-video-app")
                .build()
            Net.client.newBuilder()
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
                .newCall(request)
                .execute()
                .use { resp ->
                    if (!resp.isSuccessful) {
                        emit(DownloadState.Failed("下载失败（HTTP ${resp.code}）"))
                        return@flow
                    }
                    val input = resp.body?.byteStream() ?: run {
                        emit(DownloadState.Failed("下载失败：响应为空"))
                        return@flow
                    }
                    val total = resp.body?.contentLength() ?: -1L
                    var written = 0L
                    var lastEmit = 0L
                    input.use { src ->
                        tmp.outputStream().use { dst ->
                            val buffer = ByteArray(BUFFER_BYTES)
                            while (true) {
                                coroutineContext.ensureActive()
                                val read = src.read(buffer)
                                if (read == -1) break
                                dst.write(buffer, 0, read)
                                written += read
                                if (written - lastEmit >= EMIT_INTERVAL_BYTES) {
                                    lastEmit = written
                                    emit(DownloadState.Progress(written, total))
                                }
                            }
                        }
                    }
                    coroutineContext.ensureActive()
                    if (tmp.renameTo(target)) {
                        emit(DownloadState.Done(target))
                    } else {
                        emit(DownloadState.Failed("下载失败：文件写入异常"))
                    }
                }
        } catch (e: kotlinx.coroutines.CancellationException) {
            tmp.delete()
            throw e
        } catch (e: IOException) {
            tmp.delete()
            emit(DownloadState.Failed("网络错误：${e.message ?: "下载中断"}"))
        } catch (e: Exception) {
            tmp.delete()
            emit(DownloadState.Failed("下载失败：${e.message ?: "未知错误"}"))
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val RELEASES_API =
            "https://api.github.com/repos/Jimeng5Xingchen/mofang-video/releases?per_page=30"
        private const val TV_ASSET_PREFIX = "mofang-video-tv-"
        private const val PHONE_ASSET_PREFIX = "mofang-video-phone-"
        private const val UPDATE_DIR = "update"
        private const val APK_FILE = "update.apk"
        private const val BUFFER_BYTES = 64 * 1024
        private const val EMIT_INTERVAL_BYTES = 512 * 1024L
    }
}

/**
 * APK 安装引导：FileProvider URI + 系统安装器。
 */
object UpdateInstaller {

    fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun canInstallUnknownSources(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                context.packageManager.canRequestPackageInstalls()
}
