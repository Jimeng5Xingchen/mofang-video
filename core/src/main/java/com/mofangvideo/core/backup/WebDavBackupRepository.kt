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
        private const val KEY_AUTO = "webdav_auto_backup"
    }

    // ===== 配置 =====

    fun serverUrl(): String = prefs.getString(KEY_URL, "") ?: ""
    fun username(): String = prefs.getString(KEY_USER, "") ?: ""
    fun password(): String = prefs.getString(KEY_PASSWORD, "") ?: ""
    fun isConfigured(): Boolean =
        serverUrl().isNotEmpty() && username().isNotEmpty() && password().isNotEmpty()

    fun autoBackupEnabled(): Boolean = prefs.getBoolean(KEY_AUTO, false)

    fun setAutoBackup(enable: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO, enable).apply()
    }

    fun configure(url: String, user: String, password: String) {
        prefs.edit()
            .putString(KEY_URL, url)
            .putString(KEY_USER, user)
            .putString(KEY_PASSWORD, password)
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
    suspend fun uploadToWebDav(json: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        val request = Request.Builder()
            .url(buildBackupUrl(serverUrl(), FILE_NAME))
            .put(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Authorization", Credentials.basic(username(), password()))
            .build()
        okHttpClient.newCall(request).execute().use { resp -> resp.isSuccessful }
    }

    /**
     * 从 WebDAV 下载备份（GET）。未配置或无文件时返回 null。
     */
    suspend fun downloadFromWebDav(): String? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        val request = Request.Builder()
            .url(buildBackupUrl(serverUrl(), FILE_NAME))
            .get()
            .header("Authorization", Credentials.basic(username(), password()))
            .build()
        okHttpClient.newCall(request).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }
    }

    /**
     * 检查 WebDAV 上是否存在备份文件（PROPFIND）。
     */
    suspend fun probeWebDav(): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        val body = """<?xml version="1.0"?><D:propfind xmlns:D="DAV:"><D:prop><D:getcontentlength/></D:prop></D:propfind>"""
            .toRequestBody("application/xml; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(buildBackupUrl(serverUrl(), FILE_NAME))
            .method("PROPFIND", body)
            .header("Authorization", Credentials.basic(username(), password()))
            .header("Depth", "0")
            .build()
        okHttpClient.newCall(request).execute().use { resp ->
            resp.code == 200 || resp.code == 207
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

    private fun buildBackupUrl(server: String, file: String): String {
        val base = if (server.endsWith("/")) server else "$server/"
        return "$base$file"
    }
}

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
