package com.mofangvideo.core.repo

import com.mofangvideo.core.db.LiveChannelDao
import com.mofangvideo.core.db.LiveChannelEntity
import com.mofangvideo.core.db.LiveSourceDao
import com.mofangvideo.core.model.EpgResult
import com.mofangvideo.core.model.LiveChannel
import com.mofangvideo.core.network.Net
import com.mofangvideo.core.parser.EpgParser

/**
 * 直播频道仓库实现。
 *
 * 频道数据在直播源导入时已写入本地库，本仓库负责读取、分组过滤与 EPG 解析。
 * 已禁用的直播源下的频道不会出现在结果中。
 */
class LiveChannelRepositoryImpl(
    private val liveChannelDao: LiveChannelDao,
    private val liveSourceDao: LiveSourceDao
) : LiveChannelRepository {

    override suspend fun listByGroup(group: String): List<LiveChannel> {
        val enabledIds = liveSourceDao.listEnabled().map { it.id }.toSet()
        if (enabledIds.isEmpty()) return emptyList()

        val rows = enabledIds.flatMap { sid ->
            val all = liveChannelDao.listBySource(sid)
            if (group == ALL || group.isEmpty()) all else all.filter { it.groupName == group }
        }
        return rows.map { it.toChannel() }
    }

    override suspend fun groups(): List<String> {
        val result = mutableListOf(ALL)
        liveSourceDao.listEnabled().forEach { source ->
            liveChannelDao.listBySource(source.id)
                .map { it.groupName }
                .filter { it.isNotEmpty() }
                .forEach { if (it !in result) result.add(it) }
        }
        return result
    }

    override suspend fun epg(channel: LiveChannel): EpgResult? {
        val epgUrl = channel.epgUrl ?: return null
        if (epgUrl.isEmpty()) return null
        val raw = try {
            Net.downloadText(epgUrl)
        } catch (e: Exception) {
            return null
        }
        return EpgParser.parse(channel.tvgId, raw)
    }

    private companion object {
        const val ALL = "全部"
    }
}

private fun LiveChannelEntity.toChannel(): LiveChannel = LiveChannel(
    id = id,
    sourceId = sourceId,
    name = name,
    group = groupName,
    logo = logo,
    url = url,
    tvgId = tvgId,
    epgUrl = epgUrl
)
