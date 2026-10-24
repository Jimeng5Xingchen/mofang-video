package com.mofangvideo.core.parser

import com.mofangvideo.core.model.EpgItem
import com.mofangvideo.core.model.EpgResult
import java.util.Calendar
import java.util.TimeZone
import java.util.regex.Pattern

/**
 * 简易 XMLTV 节目单解析器。
 *
 * 输入为 XMLTV 格式文本，按 `tvg-id` 定位 `<channel>` 块，解析其中
 * `<programme start="..." stop="...">` 的起止时间与标题。
 *
 * 时间格式：`20040101120000 +0000`（含时区偏移）或 `2004-01-01T12:00:00`（ISO，按 UTC）。
 */
object EpgParser {

    fun parse(tvgId: String, raw: String): EpgResult? {
        if (tvgId.isEmpty()) return null
        return try {
            val channelBlock = channelBlock(raw, tvgId) ?: return null

            val items = mutableListOf<EpgItem>()
            val progMatcher = PROGRAMME.matcher(channelBlock)
            while (progMatcher.find()) {
                val start = parseTime(progMatcher.group(1) ?: "")
                val end = parseTime(progMatcher.group(2) ?: "")
                val titleMatcher = TITLE.matcher(progMatcher.group(3) ?: "")
                val title = if (titleMatcher.find()) titleMatcher.group(1).trim() else ""
                items += EpgItem(start = start, end = end, title = title)
            }
            if (items.isEmpty()) null else EpgResult(channelId = tvgId, items = items)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析 XMLTV 时间串为 UTC 毫秒。
     *
     * 支持两种格式：
     * - 基本格式 `20240101080000 +0800`：前导 14 位数字为本地时间，末尾时区偏移参与换算
     * - ISO 格式 `2024-01-01T12:00:00`：无偏移时按 UTC 计算
     *
     * 非法输入返回 0。
     */
    fun parseTime(raw: String): Long {
        val trimmed = raw.trim()

        isoRegex.find(trimmed)?.let { m ->
            return utcMillis(
                m.groupValues[1].toInt(),
                m.groupValues[2].toInt(),
                m.groupValues[3].toInt(),
                m.groupValues[4].toInt(),
                m.groupValues[5].toInt(),
                m.groupValues[6].toInt()
            ) - offsetMillis(trimmed)
        }

        val digits = trimmed.takeWhile { it.isDigit() }
        if (digits.length < 14) return 0L
        val millis = utcMillis(
            digits.substring(0, 4).toInt(),
            digits.substring(4, 6).toInt(),
            digits.substring(6, 8).toInt(),
            digits.substring(8, 10).toInt(),
            digits.substring(10, 12).toInt(),
            digits.substring(12, 14).toInt()
        )
        return millis - offsetMillis(trimmed)
    }

    /** 时区偏移毫秒数，如 `+0800` 为 8 小时；无偏移返回 0。 */
    private fun offsetMillis(raw: String): Long {
        val m = offsetRegex.find(raw) ?: return 0L
        val sign = if (m.groupValues[1] == "-") -1L else 1L
        val hours = m.groupValues[2].toLongOrNull() ?: return 0L
        val minutes = m.groupValues[3].toLongOrNull() ?: return 0L
        return sign * (hours * 3600000L + minutes * 60000L)
    }

    private fun utcMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.clear()
        calendar.set(year, month - 1, day, hour, minute, second)
        return calendar.timeInMillis
    }

    private val isoRegex = Regex("""(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})""")
    private val offsetRegex = Regex("""([+-])(\d{2}):?(\d{2})\s*$""")

    private fun channelBlock(raw: String, tvgId: String): String? {
        val pattern = Pattern.compile(
            "<channel[^>]*id=\"" + Pattern.quote(tvgId) + "\"[\\s\\S]*?</channel>",
            Pattern.DOTALL
        )
        val matcher = pattern.matcher(raw)
        return if (matcher.find()) matcher.group() else null
    }

    private val PROGRAMME: Pattern = Pattern.compile(
        "<programme[^>]*start=\"([^\"]*)\"[^>]*stop=\"([^\"]*)\"[^>]*>([\\s\\S]*?)</programme>",
        Pattern.DOTALL
    )
    private val TITLE: Pattern = Pattern.compile("<title[^>]*>([\\s\\S]*?)</title>")
}
