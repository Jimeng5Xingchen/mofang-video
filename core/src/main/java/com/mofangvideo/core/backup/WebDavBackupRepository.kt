package com.mofangvideo.core.backup

import android.content.Context
import android.content.SharedPreferences
import com.mofangvideo.core.db.FavoriteDao
import com.mofangvideo.core.db.FavoriteEntity
import com.mofangvideo.core.db.HistoryDao
import com.mofangvideo.core.db.LiveChannelDao
import com.mofangvideo.core.db.LiveChannelEntity
import com.mofangvideo.core.db.LiveSourceDao
import com.mofangvideo.core.db.LiveSourceEntity
import com.mofangvideo.core.db.PlayHistoryEntity
import com.mofangvideo.core.db.VideoSourceDao
import com.mofangvideo.core.db.VideoSourceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * WebDAV 备份同步仓库。
 *
 * 备份范围：影视源、直播源、直播频道、收藏、播放历史（全部本地业务数据）。
 * 云端格式：单一 JSON 文件，PUT/GET 到 WebDAV 服务器。
 * 本地格式：同一 JSON 写入 App 内部存储 filesDir/backup，支持断网恢复。
 */
class WebDavBackupRepository(
    private val prefs: SharedPreferences,
    private val videoSourceDao: VideoSourceDao,
    private val liveSourceDao: LiveSourceDao,
    private val liveChannelDao: LiveChannelDao,
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao,
    private val okHttpClient: OkHttpClient
) {

    companion object {
        const val FILE_NAME = "mofang-video-backup.json"

        private const val KEY_URL = "webdav_url"
        private const val KEY_USER = "webdav_user"
        private const val KEY_PASSWORD = "webdav_pass"
        private const val KEY_PATH = "webdav_path"
        private const val KEY_AUTO = "webdav_auto_backup"
    }

    // ===== 配置 =====

    fun serverUrl(): String = prefs.getString(KEY_URL, "") ?: ""
    fun username(): String = prefs.getString(KEY_USER, "") ?: ""
    fun password(): String = prefs.getString(KEY_PASSWORD, "") ?: ""
    fun backupPath(): String = prefs.getString(KEY_PATH, "") ?: ""
    fun isConfigured(): Boolean =
        serverUrl().isNotEmpty() && username().isNotEmpty() && password().isNotEmpty()

    fun autoBackupEnabled(): Boolean = prefs.getBoolean(KEY_AUTO, false)

    fun setAutoBackup(enable: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO, enable).apply()
    }

    fun configure(url: String, user: String, password: String, path: String = "") {
        prefs.edit()
            .putString(KEY_URL, url)
            .putString(KEY_USER, user)
            .putString(KEY_PASSWORD, password)
            .putString(KEY_PATH, path.trim().trimStart('/'))
            .apply()
    }

    // ===== 备份 =====

    /**
     * 生成备份 JSON。
     */
    suspend fun buildBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("createdAt", System.currentTimeMillis())

        root.put("videoSources", videoSourceDao.listAll().map { it.toJson() }.toJsonArray())
        root.put("liveSources", liveSourceDao.listAll().map { it.toJson() }.toJsonArray())

        val channels = mutableListOf<LiveChannelEntity>()
        liveSourceDao.listAll().forEach { source ->
            channels += liveChannelDao.listBySource(source.id)
        }
        root.put("liveChannels", channels.map { it.toJson() }.toJsonArray())

        root.put("favorites", favoriteDao.list().map { it.toJson() }.toJsonArray())
        root.put("history", historyDao.list().map { it.toJson() }.toJsonArray())

        root.toString(2)
    }

    /**
     * 上传备份到 WebDAV（PUT）。
     * 未配置服务端时返回 false。
     */
    suspend fun uploadToWebDav(json: String, fileName: String = FILE_NAME): Boolean =
        withContext(Dispatchers.IO) {
            if (!isConfigured()) return@withContext false
            ensureBackupDir()
            val request = Request.Builder()
                .url(buildBackupUrl(serverUrl(), backupPath(), fileName))
                .put(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("Authorization", Credentials.basic(username(), password()))
                .build()
            okHttpClient.newCall(request).execute().use { resp -> resp.isSuccessful }
        }

    /**
     * 从 WebDAV 下载备份（GET）。未配置或无文件时返回 null。
     */
    suspend fun downloadFromWebDav(fileName: String = FILE_NAME): String? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        val request = Request.Builder()
            .url(buildBackupUrl(serverUrl(), backupPath(), fileName))
            .get()
            .header("Authorization", Credentials.basic(username(), password()))
            .build()
        okHttpClient.newCall(request).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }
    }

    /**
     * 检查 WebDAV 上是否存在备份文件（PROPFIND Depth 0）。
     */
    suspend fun probeWebDav(): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        val body = """<?xml version="1.0"?><D:propfind xmlns:D="DAV:"><D:prop><D:getcontentlength/></D:prop></D:propfind>"""
            .toRequestBody("application/xml; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(buildBackupUrl(serverUrl(), backupPath(), FILE_NAME))
            .method("PROPFIND", body)
            .header("Authorization", Credentials.basic(username(), password()))
            .header("Depth", "0")
            .build()
        okHttpClient.newCall(request).execute().use { resp ->
            resp.code == 200 || resp.code == 207
        }
    }

    /**
     * 连接可用性检测：对配置的目录路径发 PROPFIND Depth 0。
     * 目录存在（207/200）即视为连接可用；目录不存在（404/409）时尝试创建目录后再次探测。
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        val target = buildBackupUrl(serverUrl(), backupPath(), "")
        val result = propfind(target, "0")
        when {
            result -> true
            else -> {
                ensureBackupDir()
                propfind(target, "0")
            }
        }
    }

    /**
     * 列出远程目录下全部文件（PROPFIND Depth 1）。
     */
    suspend fun listRemoteFiles(): List<RemoteBackupFile> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        val target = buildBackupUrl(serverUrl(), backupPath(), "")
        val body = """<?xml version="1.0"?><D:propfind xmlns:D="DAV:"><D:prop><D:getcontentlength/><D:getlastmodified/></D:prop></D:propfind>"""
            .toRequestBody("application/xml; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(target)
            .method("PROPFIND", body)
            .header("Authorization", Credentials.basic(username(), password()))
            .header("Depth", "1")
            .build()
        try {
            okHttpClient.newCall(request).execute().use { resp ->
                if (resp.code != 207) return@use emptyList()
                parseMultiStatus(resp.body?.string())
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun propfind(url: String, depth: String): Boolean = withContext(Dispatchers.IO) {
        val body = """<?xml version="1.0"?><D:propfind xmlns:D="DAV:"><D:prop><D:getcontentlength/></D:prop></D:propfind>"""
            .toRequestBody("application/xml; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", body)
            .header("Authorization", Credentials.basic(username(), password()))
            .header("Depth", depth)
            .build()
        try {
            okHttpClient.newCall(request).execute().use { resp ->
                resp.code == 207 || resp.code == 200
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun ensureBackupDir() {
        val dir = buildBackupUrl(serverUrl(), backupPath(), "")
        try {
            val request = Request.Builder()
                .url(dir)
                .method("MKCOL", null)
                .header("Authorization", Credentials.basic(username(), password()))
                .build()
            okHttpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            // 目录已存在或服务器不支持 MKCOL 时忽略
        }
    }

    // ===== 恢复 =====

    /**
     * 从 JSON 恢复数据：先清空本地业务数据，再逐表写入。
     */
    suspend fun restoreFromJson(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(json)
            clearAllLocal()

            root.optJSONArray("videoSources")?.let { arr ->
                for (i in 0 until arr.length()) {
                    videoSourceDao.upsert(VideoSourceEntity.fromJson(arr.getJSONObject(i)))
                }
            }
            root.optJSONArray("liveSources")?.let { arr ->
                for (i in 0 until arr.length()) {
                    liveSourceDao.upsert(LiveSourceEntity.fromJson(arr.getJSONObject(i)))
                }
            }
            root.optJSONArray("liveChannels")?.let { arr ->
                for (i in 0 until arr.length()) {
                    liveChannelDao.insertAll(listOf(LiveChannelEntity.fromJson(arr.getJSONObject(i))))
                }
            }
            root.optJSONArray("favorites")?.let { arr ->
                for (i in 0 until arr.length()) {
                    favoriteDao.insert(FavoriteEntity.fromJson(arr.getJSONObject(i)))
                }
            }
            root.optJSONArray("history")?.let { arr ->
                for (i in 0 until arr.length()) {
                    historyDao.insert(PlayHistoryEntity.fromJson(arr.getJSONObject(i)))
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun clearAllLocal() {
        videoSourceDao.listAll().forEach { videoSourceDao.delete(it.id) }
        liveSourceDao.listAll().forEach { liveSourceDao.delete(it.id) }
        favoriteDao.list().forEach { favoriteDao.delete(it.type, it.refId) }
        historyDao.list().forEach { historyDao.delete(it.id) }
    }

    // ===== 本地存储 =====

    /**
     * 写本地备份文件（App 内部存储，无运行时权限）。
     */
    suspend fun writeLocalBackup(context: Context): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "backup")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, FILE_NAME)
        file.writeText(buildBackupJson())
        file
    }

    /**
     * 读取本地备份文件。
     */
    fun readLocalBackup(context: Context): String? {
        val file = File(context.filesDir, "backup/$FILE_NAME")
        return if (file.exists()) file.readText() else null
    }

    /**
     * 上次本地备份文件信息（不存在返回 null）。
     */
    fun localBackupInfo(context: Context): LocalBackupInfo? {
        val file = File(context.filesDir, "backup/$FILE_NAME")
        return if (file.exists()) {
            LocalBackupInfo(
                fileName = FILE_NAME,
                sizeBytes = file.length(),
                modifiedAt = file.lastModified()
            )
        } else null
    }

    /** 本地备份文件信息。 */
    data class LocalBackupInfo(
        val fileName: String,
        val sizeBytes: Long,
        val modifiedAt: Long
    )

    private fun buildBackupUrl(server: String, path: String, file: String): String {
        val base = if (server.endsWith("/")) server else "$server/"
        val pathSegment = if (path.isBlank()) "" else "${path.trim('/')}/"
        return "$base$pathSegment$file"
    }

    private fun parseMultiStatus(xml: String?): List<RemoteBackupFile> {
        if (xml.isNullOrBlank()) return emptyList()
        val files = mutableListOf<RemoteBackupFile>()
        // 兼容 d:/D:/无前缀 三种命名空间写法
        val hrefRegex = Regex("<(?:[A-Za-z0-9_-]+:)?href>([^<]+)</(?:[A-Za-z0-9_-]+:)?href>")
        val lengthRegex =
            Regex("<(?:[A-Za-z0-9_-]+:)?getcontentlength>([^<]+)</(?:[A-Za-z0-9_-]+:)?getcontentlength>")
        val modifiedRegex =
            Regex("<(?:[A-Za-z0-9_-]+:)?getlastmodified>([^<]+)</(?:[A-Za-z0-9_-]+:)?getlastmodified>")
        val responseSplit = Regex("<(?:[A-Za-z0-9_-]+:)?response[ >]")
        val entries = responseSplit.split(xml).drop(1)
        for (entry in entries) {
            val href = hrefRegex.find(entry)?.groupValues?.get(1)?.trim() ?: continue
            val name = href.trim('/').substringAfterLast('/')
            if (name.isBlank()) continue
            files += RemoteBackupFile(
                name = name,
                href = href,
                sizeBytes = lengthRegex.find(entry)?.groupValues?.get(1)?.trim()?.toLongOrNull() ?: 0L,
                modifiedAt = modifiedRegex.find(entry)?.groupValues?.get(1)?.trim() ?: ""
            )
        }
        return files
    }
}

/**
 * WebDAV 远程备份文件条目。
 */
data class RemoteBackupFile(
    val name: String,
    val href: String,
    val sizeBytes: Long,
    val modifiedAt: String
)

// ===== JSON 序列化 =====

fun <T> Iterable<T>.toJsonArray(): JSONArray {
    val arr = JSONArray()
    forEach { arr.put(it) }
    return arr
}

fun VideoSourceEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("type", type)
    put("api", api)
    put("params", params)
    put("fields", fields)
    put("mappings", mappings)
    put("enabled", enabled)
    put("createdAt", createdAt)
}

fun LiveSourceEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("url", url)
    put("enabled", enabled)
    put("channelCount", channelCount)
    put("createdAt", createdAt)
}

fun LiveChannelEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("sourceId", sourceId)
    put("name", name)
    put("groupName", groupName)
    put("logo", logo)
    put("url", url)
    put("tvgId", tvgId)
    put("epgUrl", epgUrl ?: JSONObject.NULL)
}

fun FavoriteEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("type", type)
    put("refId", refId)
    put("title", title)
    put("poster", poster)
    put("createdAt", createdAt)
}

fun PlayHistoryEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("type", type)
    put("refId", refId)
    put("title", title)
    put("progressMs", progressMs)
    put("durationMs", durationMs)
    put("playedAt", playedAt)
}
