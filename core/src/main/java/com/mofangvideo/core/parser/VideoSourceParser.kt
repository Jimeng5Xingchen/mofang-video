package com.mofangvideo.core.parser

import com.mofangvideo.core.model.Episode
import com.mofangvideo.core.model.SourceType
import com.mofangvideo.core.model.Video
import com.mofangvideo.core.model.VideoParseResult
import com.mofangvideo.core.model.VideoSourceConfig
import org.json.JSONArray
import org.json.JSONObject

/**
 * 影视源解析器接口。
 *
 * 同一份源 JSON 由 [SourceType] 决定解析策略：刮削源走字段映射，自定义源走 JSONPath。
 */
interface VideoSourceParser {
    /**
     * 解析源接口返回的原始 JSON。
     * @param raw 原始 JSON 字符串
     * @param config 源配置（含字段映射）
     */
    fun parse(raw: String, config: VideoSourceConfig): VideoParseResult
}

/**
 * 刮削源解析器，按 [VideoSourceConfig.fields] 字段映射解析。
 *
 * [VideoSourceConfig.fields] 支持的键：title / poster / year / actors / summary / category / playUrl。
 * 字段缺失时降级为空字符串，单个条目异常时跳过，不中断整体解析。
 */
object ScraperSourceParser : VideoSourceParser {

    private val LIST_KEYS = listOf("list", "videos", "data")

    override fun parse(raw: String, config: VideoSourceConfig): VideoParseResult {
        val root = JSONObject(raw)
        val listArray = LIST_KEYS.firstNotNullOfOrNull { key -> root.optJSONArray(key) } ?: JSONArray()

        val f = config.fields
        val titleKey = f["title"] ?: "vod_name"
        val posterKey = f["poster"] ?: "vod_pic"
        val yearKey = f["year"] ?: "vod_year"
        val actorsKey = f["actors"] ?: "actor"
        val summaryKey = f["summary"] ?: "vod_content"
        val categoryKey = f["category"] ?: "type_name"
        val playUrlKey = f["playUrl"] ?: "vod_play_url"

        val videos = mutableListOf<Video>()
        for (i in 0 until listArray.length()) {
            val item = listArray.optJSONObject(i) ?: continue
            val episodeList = parseEpisodes(item.optString(playUrlKey, ""), i)
            videos += Video(
                id = item.optString("vod_id", i.toString()),
                sourceId = "",
                title = item.optString(titleKey, ""),
                poster = item.optString(posterKey, ""),
                year = item.optString(yearKey, ""),
                actors = item.optString(actorsKey, "")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() },
                summary = item.optString(summaryKey, ""),
                category = item.optString(categoryKey, ""),
                episodes = episodeList
            )
        }
        return VideoParseResult(videos)
    }

    /**
     * 解析播放地址串。
     * 刮削源常见格式：`名称::url#名称2::url2`，单条时既可能是 `url` 也可能是 `名称::url`。
     */
    private fun parseEpisodes(playUrl: String, itemIndex: Int): List<Episode> {
        if (playUrl.isEmpty()) return emptyList()
        val result = mutableListOf<Episode>()
        playUrl.split("#", ",").forEachIndexed { idx, entry ->
            val trimmed = entry.trim()
            if (trimmed.isEmpty()) return@forEachIndexed
            val sep = trimmed.indexOf("::")
            when {
                sep > 0 -> {
                    val name = trimmed.substring(0, sep)
                    val url = trimmed.substring(sep + 2).trim()
                    if (url.isNotEmpty()) {
                        result += Episode(id = "${itemIndex}_$idx", name = name, url = url, quality = name)
                    }
                }
                else -> result += Episode(id = "${itemIndex}_$idx", name = trimmed, url = trimmed)
            }
        }
        return result
    }
}

/**
 * 自定义源解析器，按 [VideoSourceConfig.mappings] 的 JSONPath 风格路径解析。
 *
 * 路径格式：`data.list` 定位数组，`data.list.*.url` 取每个元素的 url 字段。
 * 顶层取不到 list 时，若根对象自身含 episodes/playUrl 则视为单条影视。
 */
object CustomSourceParser : VideoSourceParser {

    override fun parse(raw: String, config: VideoSourceConfig): VideoParseResult {
        val root = JSONObject(raw)
        val m = config.mappings
        val titlePath = m["title"] ?: "title"
        val posterPath = m["poster"] ?: "poster"
        val yearPath = m["year"] ?: "year"
        val actorsPath = m["actors"] ?: "actors"
        val summaryPath = m["summary"] ?: "summary"
        val categoryPath = m["category"] ?: "category"
        val playUrlPath = m["playUrl"] ?: "playUrl"
        val episodesPath = m["episodes"] ?: "episodes"

        val listArray = root.optJSONArray("list")
            ?: root.optJSONArray("data")
            ?: if (root.has("episodes") || root.has("playUrl")) JSONArray().put(root) else JSONArray()

        val videos = mutableListOf<Video>()
        for (i in 0 until listArray.length()) {
            val item = listArray.optJSONObject(i) ?: continue
            videos += Video(
                id = item.optString("id", i.toString()),
                sourceId = "",
                title = resolve(item, titlePath),
                poster = resolve(item, posterPath),
                year = resolve(item, yearPath),
                actors = resolveList(item, actorsPath),
                summary = resolve(item, summaryPath),
                category = resolve(item, categoryPath),
                episodes = resolveEpisodes(item, episodesPath, playUrlPath, i)
            )
        }
        return VideoParseResult(videos)
    }

    /**
     * 解析剧集列表：优先按 episodesPath 取数组，回落到 playUrlPath 单地址。
     */
    private fun resolveEpisodes(item: JSONObject, episodesPath: String, playUrlPath: String, index: Int): List<Episode> {
        val array = resolveArray(item, episodesPath)
        if (array != null && array.length() > 0) {
            return (0 until array.length()).map { idx ->
                val epObj = array.optJSONObject(idx)
                if (epObj != null) {
                    Episode(
                        id = epObj.optString("id", idx.toString()),
                        name = epObj.optString("name", "第${idx + 1}集"),
                        url = epObj.optString("url", epObj.optString("playUrl", "")),
                        quality = epObj.optString("quality", "")
                    )
                } else {
                    Episode(id = idx.toString(), name = "", url = array.optString(idx, ""))
                }
            }
        }
        val single = resolve(item, playUrlPath)
        return if (single.isNotEmpty()) listOf(Episode(id = "$index-0", name = "播放", url = single)) else emptyList()
    }

    private fun resolveList(item: JSONObject, path: String): List<String> {
        return when (val value = lookup(item, path)) {
            is JSONArray -> (0 until value.length()).map { value.optString(it, "") }.filter { it.isNotEmpty() }
            is String -> value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            else -> emptyList()
        }
    }

    private fun resolve(item: JSONObject, path: String): String {
        val value = lookup(item, path) ?: return ""
        return if (value == JSONObject.NULL) "" else value.toString()
    }

    private fun resolveArray(item: JSONObject, path: String): JSONArray? =
        lookup(item, path) as? JSONArray

    /**
     * 按 JSONPath 风格路径取值，支持点号层级与 `*` 通配（`*` 取数组首元素）。
     */
    private fun lookup(root: JSONObject, path: String): Any? {
        var current: Any? = root
        for (part in path.split('.')) {
            when {
                current == null -> return null
                current == JSONObject.NULL -> return null
                part == "*" -> {
                    current = (current as? JSONArray)?.opt(0) ?: return null
                }
                current is JSONObject -> current = (current as JSONObject).opt(part)
                current is JSONArray -> {
                    val index = part.toIntOrNull()
                    current = index?.let { (current as JSONArray).opt(it) }
                }
                else -> return null
            }
        }
        return current
    }
}

/**
 * 根据源类型选择解析器。
 */
fun parserFor(type: SourceType): VideoSourceParser = when (type) {
    SourceType.SCRAPER -> ScraperSourceParser
    SourceType.CUSTOM -> CustomSourceParser
}
