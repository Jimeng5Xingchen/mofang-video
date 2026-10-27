package com.mofangvideo.core.update

/**
 * 语义化版本比较：解析 `v?MAJOR.MINOR.PATCH[-后缀]`。
 *
 * 数值逐段比较；数值相等时无后缀者更新（1.2.0 > 1.2.0-beta.1）；
 * 同带后缀时按后缀字符串比较。解析失败返回 null，调用方跳过该版本。
 */
object VersionComparator {

    private val VERSION_REGEX = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:[-.]?(.*))?$""")

    /** 解析失败返回 null。 */
    fun parse(version: String): ParsedVersion? {
        val match = VERSION_REGEX.find(version.trim()) ?: return null
        val (major, minor, patch) = match.destructured
        val numbers = listOf(major.toLongOrNull(), minor.toLongOrNull(), patch.toLongOrNull())
        if (numbers.any { it == null }) return null
        val suffix = match.groupValues[4]
        return ParsedVersion(numbers.map { it!! }, suffix.takeIf { it.isNotEmpty() })
    }

    /**
     * 比较 [candidate] 是否严格新于 [current]。
     * 任一版本不可解析时返回 false（调用方按"无法比较"跳过）。
     */
    fun isNewer(candidate: String, current: String): Boolean {
        val c = parse(candidate) ?: return false
        val cur = parse(current) ?: return false
        return when {
            c.numbers != cur.numbers -> compareNumbers(c.numbers, cur.numbers) > 0
            c.suffix == null && cur.suffix != null -> true
            c.suffix != null && cur.suffix == null -> false
            else -> compareValues(c.suffix, cur.suffix) > 0
        }
    }

    /**
     * 逐段数值比较，返回正数表示 [candidate] 版本号数值更新。
     */
    private fun compareNumbers(candidate: List<Long>, current: List<Long>): Int {
        val size = maxOf(candidate.size, current.size)
        for (i in 0 until size) {
            val a = candidate.getOrElse(i) { 0L }
            val b = current.getOrElse(i) { 0L }
            if (a != b) return if (a > b) 1 else -1
        }
        return 0
    }

    data class ParsedVersion(val numbers: List<Long>, val suffix: String?)
}
