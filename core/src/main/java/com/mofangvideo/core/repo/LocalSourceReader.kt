package com.mofangvideo.core.repo

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

/**
 * 本地源文件读取器。
 *
 * 从文件选择器拿到的 [Uri]（content:// 或 file://）读取文本内容，支持两种形态：
 * - 直接同格式文本文件（.json / .m3u）
 * - ZIP 压缩包：遍历条目，按 [preferredExtensions] 顺序取第一个匹配条目解出内容
 *
 * 自动跳过 MACOSX/__ 等压缩软件产生的元数据条目。
 */
class LocalSourceReader(private val context: Context) {

    /**
     * @param uriString 文件选择器返回的 URI 字符串
     * @param preferredExtensions 优先选取的条目后缀，不带点，如 ["json"] 或 ["m3u","m3u8"]
     * @return 文本内容（UTF-8）
     */
    fun readText(uriString: String, vararg preferredExtensions: String): String {
        val uri = Uri.parse(uriString)
        val bytes = readBytes(uri)
            ?: throw IllegalArgumentException("无法读取文件: $uriString")

        val isZip = bytes.size > 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
        return if (isZip) extractFromZip(bytes, preferredExtensions)
        else String(bytes, Charsets.UTF_8)
    }

    private fun readBytes(uri: Uri): ByteArray? {
        val resolver = context.contentResolver
        val stream = try {
            resolver.openInputStream(uri)
        } catch (e: Exception) {
            null
        } ?: return null

        stream.use { input ->
            ByteArrayOutputStream().use { out ->
                input.copyTo(out, DEFAULT_BUFFER_SIZE)
                return out.toByteArray()
            }
        }
    }

    private fun extractFromZip(bytes: ByteArray, preferredExtensions: Array<out String>): String {
        val candidates = mutableListOf<Pair<Int, ByteArray>>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                val isMeta = name.contains("__MACOSX") || entry.name.startsWith("__") ||
                    name.endsWith("/") || name.contains(".DS_Store")
                if (!isMeta) {
                    val ext = name.substringAfterLast('.', "").lowercase()
                    val rank = preferredExtensions.indexOfFirst { it.equals(ext, ignoreCase = true) }
                    if (rank >= 0) {
                        candidates += rank to readZipEntry(zip)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val best = candidates.minByOrNull { it.first }
            ?: throw IllegalArgumentException("压缩包内未找到可导入的文件（${preferredExtensions.joinToString("/")}）")
        return String(best.second, Charsets.UTF_8)
    }

    private fun readZipEntry(zip: ZipInputStream): ByteArray =
        ByteArrayOutputStream().use { out ->
            zip.copyTo(out, DEFAULT_BUFFER_SIZE)
            out.toByteArray()
        }
}
