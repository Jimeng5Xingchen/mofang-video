package com.mofangvideo.core.update

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * GitHub Releases API 响应解析与目标包选取（纯逻辑，JVM 可单测）。
 */
object ReleaseParser {

    private const val MAX_NOTES_LENGTH = 2000

    /** 解析 releases 数组 JSON，逐项容错：坏项跳过。 */
    fun parseReleases(json: String): List<ReleaseInfo> {
        val array = try {
            JSONArray(json)
        } catch (e: JSONException) {
            return emptyList()
        }
        val releases = mutableListOf<ReleaseInfo>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val tag = obj.optString("tag_name")
            if (tag.isEmpty()) continue
            releases += ReleaseInfo(
                tagName = tag,
                name = obj.optString("name").ifEmpty { tag },
                isPrerelease = obj.optBoolean("prerelease", false),
                body = obj.optString("body").take(MAX_NOTES_LENGTH),
                assets = parseAssets(obj.optJSONArray("assets"))
            )
        }
        return releases
    }

    private fun parseAssets(array: JSONArray?): List<ReleaseAsset> {
        if (array == null) return emptyList()
        val assets = mutableListOf<ReleaseAsset>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val name = obj.optString("name")
            val url = obj.optString("browser_download_url")
            if (name.isEmpty() || url.isEmpty()) continue
            assets += ReleaseAsset(name, url, obj.optLong("size", 0L))
        }
        return assets
    }

    /**
     * 按渠道筛选并挑选第一个「新于 [currentVersion]」且含 [assetPrefix] APK 资产的 Release。
     *
     * @return 命中的 [AppUpdate]；无新版本或资产缺失返回 null。
     */
    fun pickUpdate(
        releases: List<ReleaseInfo>,
        channel: UpdateChannel,
        currentVersion: String,
        assetPrefix: String
    ): AppUpdate? {
        val candidates = releases
            .filter { channel == UpdateChannel.BETA || !it.isPrerelease }
            .filter { VersionComparator.isNewer(it.tagName, currentVersion) }
        for (release in candidates) {
            val apk = release.assets.firstOrNull {
                it.name.startsWith(assetPrefix) && it.name.endsWith(".apk")
            } ?: continue
            return AppUpdate(
                versionName = release.tagName.removePrefix("v"),
                isPrerelease = release.isPrerelease,
                releaseNotes = release.body,
                apkUrl = apk.downloadUrl,
                apkName = apk.name,
                apkSizeBytes = apk.sizeBytes
            )
        }
        return null
    }
}
