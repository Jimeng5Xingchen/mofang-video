package com.mofangvideo.core.repo

import com.mofangvideo.core.db.VideoSourceDao
import com.mofangvideo.core.db.VideoSourceEntity
import com.mofangvideo.core.model.SourceType
import com.mofangvideo.core.model.Video
import com.mofangvideo.core.model.VideoSourceConfig
import com.mofangvideo.core.network.Net
import com.mofangvideo.core.parser.parserFor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject

/**
 * 影视数据仓库实现。
 *
 * 在全部启用源上并发搜索/详情，聚合结果，单源失败自动跳过不影响整体。
 */
class VideoRepositoryImpl(
    private val videoSourceDao: VideoSourceDao
) : VideoRepository {

    override suspend fun search(keyword: String, category: String): List<Video> {
        val enabled = videoSourceDao.listEnabled().map { it.toConfig() }
        if (enabled.isEmpty()) return emptyList()
        if (keyword.isBlank()) return emptyList()
        return coroutineScope {
            enabled.map { source ->
                async {
                    try {
                        val raw = Net.downloadText("${source.api}?ac=detail&wd=${encode(keyword)}")
                        parserFor(source.type).parse(raw, source).videos
                            .map { it.copy(sourceId = source.id) }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }.awaitAll().flatten()
        }
    }

    override suspend fun detail(sourceId: String, videoId: String): Video? {
        val source = videoSourceDao.findById(sourceId)?.toConfig() ?: return null
        val raw = try {
            Net.downloadText("${source.api}?ac=detail&ids=$videoId")
        } catch (e: Exception) {
            return null
        }
        val videos = parserFor(source.type).parse(raw, source).videos
            .map { it.copy(sourceId = source.id) }
        return videos.firstOrNull { it.id == videoId } ?: videos.firstOrNull()
    }

    private fun encode(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")
}

private fun VideoSourceEntity.toConfig(): VideoSourceConfig {
    fun mapOf(s: String): Map<String, String> = try {
        JSONObject(s).let { jo ->
            jo.keys().asSequence().associate { key -> key to jo.optString(key, "") }
        }
    } catch (e: Exception) {
        emptyMap()
    }

    return VideoSourceConfig(
        id = id,
        name = name,
        type = SourceType.valueOf(type),
        api = api,
        params = mapOf(params),
        fields = mapOf(fields),
        mappings = mapOf(mappings),
        enabled = enabled
    )
}
