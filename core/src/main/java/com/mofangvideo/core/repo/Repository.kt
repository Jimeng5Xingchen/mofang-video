package com.mofangvideo.core.repo

import com.mofangvideo.core.model.EpgResult
import com.mofangvideo.core.model.FavType
import com.mofangvideo.core.model.Favorite
import com.mofangvideo.core.model.LiveChannel
import com.mofangvideo.core.model.LiveSourceConfig
import com.mofangvideo.core.model.PlayHistory
import com.mofangvideo.core.model.Video
import com.mofangvideo.core.model.VideoSourceConfig
import kotlinx.coroutines.flow.Flow

/**
 * 结果包装。
 */
sealed class Result<out T> {
    data class Ok<T>(val value: T) : Result<T>()
    data class Err(val message: String) : Result<Nothing>()

    inline fun <R> map(transform: (T) -> R): Result<R> =
        if (this is Ok) Ok(transform(value)) else this as Result<R>

    fun isOk(): Boolean = this is Ok
}

/**
 * 影视源仓库。
 */
interface VideoSourceRepository {
    suspend fun importFromFile(uri: String): Result<VideoSourceConfig>
    suspend fun importFromUrl(url: String, name: String): Result<VideoSourceConfig>
    suspend fun listAll(): Flow<List<VideoSourceConfig>>
    suspend fun toggle(sourceId: String, enabled: Boolean)
    suspend fun delete(sourceId: String)
}

/**
 * 影视数据仓库。
 */
interface VideoRepository {
    suspend fun search(keyword: String, category: String = ""): List<Video>
    suspend fun detail(sourceId: String, videoId: String): Video?
}

/**
 * M3U 直播源仓库。
 */
interface LiveSourceRepository {
    suspend fun importFromFile(uri: String, name: String): Result<LiveSourceConfig>
    suspend fun importFromUrl(url: String, name: String): Result<LiveSourceConfig>
    suspend fun listAll(): Flow<List<LiveSourceConfig>>
    suspend fun toggle(sourceId: String, enabled: Boolean)
    suspend fun delete(sourceId: String)
}

/**
 * 直播频道仓库。
 */
interface LiveChannelRepository {
    suspend fun listByGroup(group: String): List<LiveChannel>
    suspend fun groups(): List<String>
    suspend fun epg(channel: LiveChannel): EpgResult?
}

/**
 * 备份仓库：本地数据备份/恢复到 WebDAV 或本地文件。
 */
interface BackupRepository {
    // 配置
    fun serverUrl(): String
    fun username(): String
    fun backupPath(): String
    fun isConfigured(): Boolean
    fun autoBackupEnabled(): Boolean
    fun setAutoBackup(enable: Boolean)
    fun configure(url: String, user: String, password: String, path: String)

    // 连接检测
    suspend fun testConnection(): Result<Boolean>

    // WebDAV 备份/恢复
    suspend fun backupToWebDav(): Result<Boolean>
    suspend fun restoreFromWebDav(): Result<Boolean>
    suspend fun listWebDavFiles(): Result<List<RemoteBackupFile>>
    suspend fun restoreFromWebDavFile(fileName: String): Result<Boolean>

    // 本地备份/恢复
    suspend fun backupToLocal(): Result<String>
    suspend fun restoreFromLocal(): Result<Boolean>
    fun localBackupInfo(): LocalBackupInfo?

    // 兼容旧接口（UI 旧调用）
    suspend fun probeWebDav(): Result<Boolean>
}

/**
 * WebDAV 远程备份文件条目（核心层透出，UI 展示用）。
 */
data class RemoteBackupFile(
    val name: String,
    val sizeBytes: Long,
    val modifiedAt: String
)

/**
 * 本地备份文件信息。
 */
data class LocalBackupInfo(
    val fileName: String,
    val sizeBytes: Long,
    val modifiedAt: Long
)

/**
 * 收藏仓库。
 */
interface FavoriteRepository {
    suspend fun add(type: FavType, refId: String, title: String, poster: String = "")
    suspend fun remove(type: FavType, refId: String)
    suspend fun list(type: FavType? = null): List<Favorite>
    suspend fun isFavorited(type: FavType, refId: String): Boolean
}

/**
 * 播放历史仓库。
 */
interface HistoryRepository {
    suspend fun record(type: FavType, refId: String, title: String, progressMs: Long, durationMs: Long)
    suspend fun list(): List<PlayHistory>
    suspend fun latest(type: FavType, refId: String): PlayHistory?
    suspend fun delete(id: Long)
    suspend fun clearAll()
}

/**
 * 设置仓库。
 */
interface SettingsRepository {
    fun playbackCore(): Flow<String>
    fun setPlaybackCore(core: String)
    fun autoResume(): Flow<Boolean>
    fun setAutoResume(enable: Boolean)
    fun defaultQuality(): Flow<String>
    fun setDefaultQuality(quality: String)
    fun updateChannel(): Flow<String>
    fun setUpdateChannel(channel: String)
    fun themeMode(): Flow<String>
    fun setThemeMode(mode: String)
}
