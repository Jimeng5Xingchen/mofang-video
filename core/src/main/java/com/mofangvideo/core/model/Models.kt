package com.mofangvideo.core.model

/**
 * 影视源类型。
 * [SCRAPER] 刮削源，按 fields 字段映射解析；[CUSTOM] 自定义源，按 mappings JSONPath 解析。
 */
enum class SourceType { SCRAPER, CUSTOM }

/**
 * 收藏类型，区分影视与直播频道。
 */
enum class FavType { VIDEO, LIVE }

/**
 * 影视源配置。
 *
 * @param id 源唯一标识
 * @param name 源名称（必填）
 * @param type 源类型
 * @param api 接口地址（必填）
 * @param params 通用请求参数
 * @param fields 刮削字段映射（仅 [SourceType.SCRAPER]）
 * @param mappings 自定义字段映射（仅 [SourceType.CUSTOM]）
 * @param enabled 是否启用
 */
data class VideoSourceConfig(
    val id: String,
    val name: String,
    val type: SourceType,
    val api: String,
    val params: Map<String, String> = emptyMap(),
    val fields: Map<String, String> = emptyMap(),
    val mappings: Map<String, String> = emptyMap(),
    val enabled: Boolean = true
)

/**
 * 影视实体（聚合后）。
 */
data class Video(
    val id: String,
    val sourceId: String,
    val title: String,
    val poster: String,
    val year: String,
    val actors: List<String> = emptyList(),
    val summary: String = "",
    val category: String = "",
    val episodes: List<Episode> = emptyList()
)

/**
 * 单集。
 * @param quality 清晰度标签
 * @param duration 时长毫秒
 */
data class Episode(
    val id: String,
    val name: String,
    val url: String,
    val quality: String = "",
    val duration: Long = 0L
)

/**
 * M3U 直播源配置。
 * @param url m3u 文件地址
 * @param channelCount 解析后频道数量
 */
data class LiveSourceConfig(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val channelCount: Int = 0
)

/**
 * 直播频道。
 * @param group 分组名（来自 tvg-group）
 * @param tvgId tvg-id
 * @param epgUrl 节目单地址
 */
data class LiveChannel(
    val id: String,
    val sourceId: String,
    val name: String,
    val group: String = "",
    val logo: String = "",
    val url: String,
    val tvgId: String = "",
    val epgUrl: String? = null
)

/**
 * EPG 节目项。
 */
data class EpgItem(
    val start: Long,
    val end: Long,
    val title: String
)

/**
 * 节目单解析结果。
 */
data class EpgResult(
    val channelId: String,
    val items: List<EpgItem>
)

/**
 * 收藏记录。
 */
data class Favorite(
    val id: Long = 0L,
    val type: FavType,
    val refId: String,
    val title: String,
    val poster: String = "",
    val createdAt: Long = 0L
)

/**
 * 播放历史。
 * @param progressMs 已播放进度毫秒
 * @param durationMs 总时长毫秒
 */
data class PlayHistory(
    val id: Long = 0L,
    val type: FavType,
    val refId: String,
    val title: String,
    val progressMs: Long = 0L,
    val durationMs: Long = 0L,
    val playedAt: Long = 0L
)

/**
 * M3U 解析结果。
 * @param channels 解析出的频道
 * @param skippedCount 跳过的无效行数
 */
data class M3uParseResult(
    val channels: List<LiveChannel>,
    val skippedCount: Int
)

/**
 * 影视解析结果。
 */
data class VideoParseResult(
    val videos: List<Video>
)

/**
 * 通用解析失败原因。
 */
sealed class ParseError {
    class InvalidJson(val raw: String, val reason: String) : ParseError()
    class MissingField(val field: String) : ParseError()
    object Timeout : ParseError()
}
