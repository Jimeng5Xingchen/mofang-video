package com.mofangvideo.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseParserTest {

    private val releasesJson = """
        [
          {
            "tag_name": "v1.1.0",
            "name": "v1.1.0 稳定版",
            "prerelease": false,
            "body": "新增检查更新",
            "assets": [
              {"name": "mofang-video-phone-v1.1.0-a893a0f.apk", "browser_download_url": "https://x/phone.apk", "size": 8800000},
              {"name": "mofang-video-tv-v1.1.0-a893a0f.apk", "browser_download_url": "https://x/tv.apk", "size": 9300000},
              {"name": "SHA256SUMS.txt", "browser_download_url": "https://x/sha.txt", "size": 512}
            ]
          },
          {
            "tag_name": "v1.1.0-beta.1",
            "name": "Beta",
            "prerelease": true,
            "body": "内测",
            "assets": [
              {"name": "mofang-video-phone-v1.1.0-beta.1-1234567.apk", "browser_download_url": "https://x/phone-beta.apk", "size": 8800000},
              {"name": "mofang-video-tv-v1.1.0-beta.1-1234567.apk", "browser_download_url": "https://x/tv-beta.apk", "size": 9300000}
            ]
          },
          {
            "tag_name": "not-a-version",
            "prerelease": false,
            "assets": []
          }
        ]
    """.trimIndent()

    @Test
    fun `解析全部 release 且跳过坏 tag 项`() {
        val releases = ReleaseParser.parseReleases(releasesJson)
        assertEquals(3, releases.size)
        assertEquals("v1.1.0", releases[0].tagName)
        assertFalse(releases[0].isPrerelease)
        assertEquals(3, releases[0].assets.size)
    }

    @Test
    fun `正式版渠道过滤预发布`() {
        val releases = ReleaseParser.parseReleases(releasesJson)
        val update = ReleaseParser.pickUpdate(releases, UpdateChannel.STABLE, "1.0.0", "mofang-video-phone-")
        assertEquals("1.1.0", update?.versionName)
        assertFalse(update?.isPrerelease ?: true)
        assertEquals("https://x/phone.apk", update?.apkUrl)
    }

    @Test
    fun `Beta 渠道包含预发布且取最新命中`() {
        val releases = ReleaseParser.parseReleases(releasesJson)
        val update = ReleaseParser.pickUpdate(releases, UpdateChannel.BETA, "1.0.0", "mofang-video-tv-")
        assertEquals("1.1.0", update?.versionName)
        assertEquals("https://x/tv.apk", update?.apkUrl)
    }

    @Test
    fun `设备前缀决定目标包`() {
        val releases = ReleaseParser.parseReleases(releasesJson)
        val tv = ReleaseParser.pickUpdate(releases, UpdateChannel.BETA, "1.0.0", "mofang-video-tv-")
        assertEquals("https://x/tv.apk", tv?.apkUrl)
        val phone = ReleaseParser.pickUpdate(releases, UpdateChannel.BETA, "1.0.0", "mofang-video-phone-")
        assertEquals("https://x/phone.apk", phone?.apkUrl)
        val unknown = ReleaseParser.pickUpdate(releases, UpdateChannel.BETA, "1.0.0", "mofang-video-pad-")
        assertNull(unknown)
        val alreadyBeta = ReleaseParser.pickUpdate(releases, UpdateChannel.BETA, "1.1.0", "mofang-video-tv-")
        assertNull(alreadyBeta)
    }

    @Test
    fun `当前已是最新返回 null`() {
        val releases = ReleaseParser.parseReleases(releasesJson)
        assertNull(ReleaseParser.pickUpdate(releases, UpdateChannel.BETA, "1.1.0", "mofang-video-phone-"))
        assertNull(ReleaseParser.pickUpdate(releases, UpdateChannel.STABLE, "2.0.0", "mofang-video-phone-"))
    }

    @Test
    fun `目标资产缺失返回 null`() {
        val json = """
            [{"tag_name": "v2.0.0", "prerelease": false, "assets": [
              {"name": "SHA256SUMS.txt", "browser_download_url": "https://x/sha.txt", "size": 1}
            ]}]
        """.trimIndent()
        val releases = ReleaseParser.parseReleases(json)
        assertNull(ReleaseParser.pickUpdate(releases, UpdateChannel.STABLE, "1.0.0", "mofang-video-phone-"))
    }

    @Test
    fun `非法 JSON 返回空列表`() {
        assertTrue(ReleaseParser.parseReleases("{broken").isEmpty())
    }
}
