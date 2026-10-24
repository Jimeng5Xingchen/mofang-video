package com.mofangvideo.core.parser

import com.mofangvideo.core.model.LiveChannel
import com.mofangvideo.core.model.M3uParseResult

/**
 * M3U/M3U8 解析器，支持标准格式与扩展 tvg-* 字段。
 *
 * 标准格式：
 * ```
 * #EXTM3U
 * #EXTINF:-1,频道名称
 * http://example.com/stream.m3u8
 * ```
 *
 * 扩展格式：
 * ```
 * #EXTM3U
 * #EXTINF:-1 tvg-id="CH001" tvg-name="频道A" tvg-logo="http://.../logo.png" tvg-group="体育",频道A
 * http://example.com/stream.m3u8
 * ```
 *
 * 解析规则：
 * - 逐行扫描，识别 #EXTINF 行提取元数据，紧随其后的非注释行作为 URL
 * - tvg-group 用于分组归类，缺失时归为默认分组
 * - 紧随 #EXTINF 的地址行须包含 "://"（协议分隔），否则计入跳过
 * - 无效行（非空、非 #EXTINF 且无协议的行）计入 [M3uParseResult.skippedCount]
 * - 解析幂等：相同输入产生相同输出
 */
object M3uParser {

    private const val EXT_M3U = "#EXTM3U"
    private const val EXT_INF = "#EXTINF"

    /**
     * 解析 M3U 内容。
     *
     * @param content M3U 文件全文
     * @return 解析结果，含频道列表与跳过无效行数
     */
    fun parse(content: String): M3uParseResult {
        val lines = content.lines()
        val channels = mutableListOf<LiveChannel>()
        var skippedCount = 0

        var pendingMeta: M3uMeta? = null

        lines.forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.isEmpty() -> {
                    // 空行不计入跳过
                }
                line.startsWith(EXT_M3U) -> {
                    // 头部行，忽略
                }
                line.startsWith(EXT_INF) -> {
                    pendingMeta = parseExtInf(line)
                }
                line.startsWith("#") -> {
                    // 其他注释行（如 #EXTVLCOPT）跳过
                    skippedCount++
                }
                else -> {
                    // URL 行：仅接受带协议的地址，其余行视为无效
                    val meta = pendingMeta ?: M3uMeta()
                    pendingMeta = null
                    if (line.contains("://")) {
                        val id = "${meta.tvgId}_${channels.size}"
                        channels += LiveChannel(
                            id = id,
                            sourceId = "", // 由调用方回填
                            name = meta.name,
                            group = meta.group,
                            logo = meta.logo,
                            url = line,
                            tvgId = meta.tvgId,
                            epgUrl = meta.epg
                        )
                    } else {
                        skippedCount++
                    }
                }
            }
        }

        return M3uParseResult(channels = channels, skippedCount = skippedCount)
    }

    /**
     * 解析 #EXTINF 行的元数据。
     *
     * 提取 tvg-id / tvg-name / tvg-logo / tvg-group / tvg-epg 属性，
     * 逗号后为频道名称。无属性时退化为纯文本名称。
     */
    private fun parseExtInf(line: String): M3uMeta {
        // 去掉 #EXTINF: 前缀
        val rest = line.removePrefix(EXT_INF).removePrefix(":")
        // 逗号前为属性，逗号后为名称
        val commaIdx = rest.indexOf(',')
        val attrPart = if (commaIdx > 0) rest.substring(0, commaIdx) else rest
        val namePart = if (commaIdx > 0) rest.substring(commaIdx + 1).trim() else ""

        val tvgId = attrPart.attr("tvg-id")
        val tvgName = attrPart.attr("tvg-name")
        val logo = attrPart.attr("tvg-logo")
        val group = attrPart.attr("tvg-group") ?: DEFAULT_GROUP
        val epg = attrPart.attr("tvg-epg")
        val name = if (namePart.isNotEmpty()) namePart else tvgName.orEmpty()

        return M3uMeta(
            name = name,
            tvgId = tvgId.orEmpty(),
            logo = logo.orEmpty(),
            group = group,
            epg = epg
        )
    }

    /**
     * 从属性串中提取键值。
     * 键值格式：`key="value"` 或 `key=value`。
     */
    private fun String.attr(key: String): String? {
        val trimmed = this.trim()
        // 匹配 key="value" 或 key=value
        val regex = Regex("""(?:^|\s)""" + Regex.escape(key) + """\s*=\s*"([^"]*)"""")
        val quoted = regex.find(trimmed)?.groupValues?.get(1)
        if (quoted != null) return quoted

        val plain = Regex("""(?:^|\s)""" + Regex.escape(key) + """\s*=\s*(\S+)""")
        return plain.find(trimmed)?.groupValues?.get(1)
    }

    private const val DEFAULT_GROUP = "默认"

    private data class M3uMeta(
        val name: String = "",
        val tvgId: String = "",
        val logo: String = "",
        val group: String = DEFAULT_GROUP,
        val epg: String? = null
    )
}
