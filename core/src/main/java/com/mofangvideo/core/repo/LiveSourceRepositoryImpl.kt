package com.mofangvideo.core.repo

import com.mofangvideo.core.db.LiveChannelDao
import com.mofangvideo.core.db.LiveChannelEntity
import com.mofangvideo.core.db.LiveSourceDao
import com.mofangvideo.core.db.LiveSourceEntity
import com.mofangvideo.core.db.MofangDatabase
import com.mofangvideo.core.model.LiveSourceConfig
import com.mofangvideo.core.network.Net
import com.mofangvideo.core.parser.M3uParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * M3U 直播源仓库实现。
 *
 * 导入时下载 M3U 内容、用 [M3uParser] 解析、写入频道表并统计跳过行数。
 */
class LiveSourceRepositoryImpl(
    private val db: MofangDatabase,
    private val liveSourceDao: LiveSourceDao,
    private val liveChannelDao: LiveChannelDao,
    private val localReader: LocalSourceReader
) : LiveSourceRepository {

    private val sources = MutableStateFlow<List<LiveSourceConfig>>(emptyList())

    override suspend fun listAll(): Flow<List<LiveSourceConfig>> {
        sources.value = loadAll()
        return sources.asStateFlow()
    }

    override suspend fun importFromFile(uri: String, name: String): Result<LiveSourceConfig> {
        val content = try {
            localReader.readText(uri, "m3u", "m3u8", "txt", "json")
        } catch (e: Exception) {
            return Result.Err("读取文件失败: ${e.message}")
        }
        return parseAndSave(content, name, "file://$uri")
    }

    override suspend fun importFromUrl(url: String, name: String): Result<LiveSourceConfig> {
        val content = try {
            Net.downloadText(url)
        } catch (e: Exception) {
            return Result.Err("下载失败: ${e.message}")
        }
        return parseAndSave(content, name, url)
    }

    override suspend fun toggle(sourceId: String, enabled: Boolean) {
        val count = liveChannelDao.listBySource(sourceId).size
        liveSourceDao.setEnabled(sourceId, enabled, count)
        sources.value = loadAll()
    }

    override suspend fun delete(sourceId: String) {
        liveChannelDao.deleteBySource(sourceId)
        liveSourceDao.delete(sourceId)
        sources.value = loadAll()
    }

    private suspend fun parseAndSave(content: String, name: String, url: String): Result<LiveSourceConfig> {
        val parsed = M3uParser.parse(content)
        if (parsed.channels.isEmpty()) {
            return Result.Err("未解析到有效频道，跳过 ${parsed.skippedCount} 行")
        }

        val sourceId = UUID.randomUUID().toString()
        return try {
            liveSourceDao.upsert(
                LiveSourceEntity(
                    id = sourceId,
                    name = name,
                    url = url,
                    enabled = true,
                    channelCount = parsed.channels.size,
                    createdAt = System.currentTimeMillis()
                )
            )
            liveChannelDao.insertAll(
                parsed.channels.map { ch ->
                    LiveChannelEntity(
                        id = ch.id,
                        sourceId = sourceId,
                        name = ch.name,
                        groupName = ch.group,
                        logo = ch.logo,
                        url = ch.url,
                        tvgId = ch.tvgId,
                        epgUrl = ch.epgUrl
                    )
                }
            )
            sources.value = loadAll()
            Result.Ok(
                LiveSourceConfig(
                    id = sourceId,
                    name = name,
                    url = url,
                    enabled = true,
                    channelCount = parsed.channels.size
                )
            )
        } catch (e: Exception) {
            Result.Err("保存失败: ${e.message}")
        }
    }

    private suspend fun loadAll(): List<LiveSourceConfig> =
        liveSourceDao.listAll().map { it.toConfig() }
}

private fun LiveSourceEntity.toConfig(): LiveSourceConfig = LiveSourceConfig(
    id = id,
    name = name,
    url = url,
    enabled = enabled,
    channelCount = channelCount
)
