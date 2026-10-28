package com.mofangvideo.core.repo

import com.mofangvideo.core.db.MofangDatabase
import com.mofangvideo.core.db.VideoSourceDao
import com.mofangvideo.core.db.VideoSourceEntity
import com.mofangvideo.core.model.SourceType
import com.mofangvideo.core.model.VideoSourceConfig
import com.mofangvideo.core.network.Net
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

/**
 * 影视源仓库实现。
 *
 * 导入时解析 JSON 配置并校验必填字段（api），写入本地数据库并以 [Flow] 暴露列表。
 */
class VideoSourceRepositoryImpl(
    private val db: MofangDatabase,
    private val videoSourceDao: VideoSourceDao,
    private val localReader: LocalSourceReader
) : VideoSourceRepository {

    private val sources = MutableStateFlow<List<VideoSourceConfig>>(emptyList())

    override suspend fun listAll(): Flow<List<VideoSourceConfig>> {
        sources.value = loadAll()
        return sources.asStateFlow()
    }

    override suspend fun importFromFile(uri: String): Result<VideoSourceConfig> {
        val content = try {
            localReader.readText(uri, "json")
        } catch (e: Exception) {
            return Result.Err("读取文件失败: ${e.message}")
        }
        return parseAndSave(content, nameFromUri(uri))
    }

    override suspend fun importFromUrl(url: String, name: String): Result<VideoSourceConfig> {
        val content = try {
            Net.downloadText(url)
        } catch (e: Exception) {
            return Result.Err("下载失败: ${e.message}")
        }
        return parseAndSave(content, name)
    }

    override suspend fun toggle(sourceId: String, enabled: Boolean) {
        videoSourceDao.setEnabled(sourceId, enabled)
        sources.value = loadAll()
    }

    override suspend fun delete(sourceId: String) {
        videoSourceDao.delete(sourceId)
        sources.value = loadAll()
    }

    private suspend fun parseAndSave(raw: String, name: String): Result<VideoSourceConfig> {
        val json = try {
            JSONObject(raw)
        } catch (e: Exception) {
            return Result.Err("JSON 格式错误")
        }

        val api = json.optString("api", "")
        if (api.isEmpty()) return Result.Err("缺少必填字段 api")

        val sourceName = json.optString("name", "").takeIf { it.isNotEmpty() } ?: name
        val type = when (json.optString("type", "scraper").lowercase()) {
            "custom" -> SourceType.CUSTOM
            else -> SourceType.SCRAPER
        }

        val config = VideoSourceConfig(
            id = UUID.randomUUID().toString(),
            name = sourceName,
            type = type,
            api = api,
            params = json.optJSONObject("params").toMap(),
            fields = json.optJSONObject("fields").toMap(),
            mappings = json.optJSONObject("mappings").toMap(),
            enabled = json.optBoolean("enabled", true)
        )
        return save(config)
    }

    private suspend fun save(config: VideoSourceConfig): Result<VideoSourceConfig> {
        try {
            videoSourceDao.upsert(
                VideoSourceEntity(
                    id = config.id,
                    name = config.name,
                    type = config.type.name,
                    api = config.api,
                    params = config.params.toJson(),
                    fields = config.fields.toJson(),
                    mappings = config.mappings.toJson(),
                    enabled = config.enabled,
                    createdAt = System.currentTimeMillis()
                )
            )
            sources.value = loadAll()
            return Result.Ok(config)
        } catch (e: Exception) {
            return Result.Err("保存失败: ${e.message}")
        }
    }

    private suspend fun loadAll(): List<VideoSourceConfig> = withContext(Dispatchers.IO) {
        try {
            videoSourceDao.listAll().map { it.toConfig() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun nameFromUri(uri: String): String {
        val name = uri.substringAfterLast('/').substringBeforeLast('.')
        return if (name.isNotEmpty()) name else "影视源"
    }
}

/** Map 转 JSON 字符串，空 map 输出 `{}`。 */
internal fun Map<String, String>.toJson(): String =
    if (isEmpty()) "{}" else JSONObject(this).toString()

/** JSONObject 转 Map，解析失败时返回空 map。 */
internal fun JSONObject?.toMap(): Map<String, String> {
    if (this == null) return emptyMap()
    return try {
        val result = mutableMapOf<String, String>()
        keys().forEach { key -> result[key] = optString(key, "") }
        result
    } catch (e: Exception) {
        emptyMap()
    }
}

private fun VideoSourceEntity.toConfig(): VideoSourceConfig = VideoSourceConfig(
    id = id,
    name = name,
    type = SourceType.valueOf(type),
    api = api,
    params = runCatching { JSONObject(params) }.getOrNull().toMap(),
    fields = runCatching { JSONObject(fields) }.getOrNull().toMap(),
    mappings = runCatching { JSONObject(mappings) }.getOrNull().toMap(),
    enabled = enabled
)
