package com.mofangvideo.core.parser

import com.mofangvideo.core.model.SourceType
import com.mofangvideo.core.model.VideoSourceConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomSourceParserTest {

    private val config = VideoSourceConfig(
        id = "src2",
        name = "自定义源",
        type = SourceType.CUSTOM,
        api = "https://example.com/custom.json"
    )

    @Test
    fun `parses flat list with default mapping`() {
        val raw = """
            {
              "list": [
                { "id": "v1", "title": "影片一", "poster": "http://p/1.jpg", "year": "2021", "summary": "简介", "category": "剧情",
                  "episodes": [ { "id": "e1", "name": "正片", "url": "http://s/1.m3u8" } ] }
              ]
            }
        """.trimIndent()

        val result = CustomSourceParser.parse(raw, config)

        val video = result.videos.single()
        assertEquals("v1", video.id)
        assertEquals("影片一", video.title)
        assertEquals("http://p/1.jpg", video.poster)
        assertEquals("2021", video.year)
        assertEquals("简介", video.summary)
        assertEquals("剧情", video.category)
        assertEquals(1, video.episodes.size)
        assertEquals("正片", video.episodes[0].name)
        assertEquals("http://s/1.m3u8", video.episodes[0].url)
    }

    @Test
    fun `dotted mapping paths resolve nested fields`() {
        val config = config.copy(
            mappings = mapOf(
                "title" to "meta.name",
                "poster" to "meta.cover",
                "year" to "meta.info.year",
                "episodes" to "media.list"
            )
        )
        val raw = """
            {
              "list": [
                {
                  "meta": { "name": "嵌套片", "cover": "http://p/n.jpg", "info": { "year": "2022" } },
                  "media": { "list": [ { "name": "第1集", "playUrl": "http://s/n1.m3u8" } ] }
                }
              ]
            }
        """.trimIndent()

        val video = CustomSourceParser.parse(raw, config).videos.single()

        assertEquals("嵌套片", video.title)
        assertEquals("http://p/n.jpg", video.poster)
        assertEquals("2022", video.year)
        assertEquals("第1集", video.episodes.single().name)
        assertEquals("http://s/n1.m3u8", video.episodes.single().url)
    }

    @Test
    fun `wildcard path takes first array element`() {
        val config = config.copy(mappings = mapOf("title" to "items.*.title"))
        val raw = """
            { "list": [ { "items": [ { "title": "首元素" }, { "title": "次元素" } ] } ] }
        """.trimIndent()

        assertEquals("首元素", CustomSourceParser.parse(raw, config).videos.single().title)
    }

    @Test
    fun `root object with episodes is treated as single video`() {
        val raw = """
            {
              "id": "solo",
              "title": "单片",
              "episodes": [ { "name": "正片", "url": "http://s/solo.m3u8" } ]
            }
        """.trimIndent()

        val result = CustomSourceParser.parse(raw, config)

        assertEquals(1, result.videos.size)
        assertEquals("单片", result.videos[0].title)
        assertEquals("http://s/solo.m3u8", result.videos[0].episodes.single().url)
    }

    @Test
    fun `episodes fallback to playUrl single address`() {
        val raw = """
            { "list": [ { "id": "p1", "title": "单地址片", "playUrl": "http://s/p1.mp4" } ] }
        """.trimIndent()

        val episodes = CustomSourceParser.parse(raw, config).videos.single().episodes

        assertEquals(1, episodes.size)
        assertEquals("http://s/p1.mp4", episodes[0].url)
    }

    @Test
    fun `string episode array entries become url only episodes`() {
        val raw = """
            { "list": [ { "id": "a", "title": "字符串剧集", "episodes": [ "http://s/1.m3u8", "http://s/2.m3u8" ] } ] }
        """.trimIndent()

        val episodes = CustomSourceParser.parse(raw, config).videos.single().episodes

        assertEquals(2, episodes.size)
        assertEquals("http://s/1.m3u8", episodes[0].url)
        assertEquals("http://s/2.m3u8", episodes[1].url)
    }

    @Test
    fun `actors accept both array and comma string`() {
        val rawArray = """
            { "list": [ { "id": "1", "title": "A", "actors": ["甲", "乙"] } ] }
        """.trimIndent()
        val rawString = """
            { "list": [ { "id": "2", "title": "B", "actors": "丙,丁" } ] }
        """.trimIndent()

        assertEquals(listOf("甲", "乙"), CustomSourceParser.parse(rawArray, config).videos.single().actors)
        assertEquals(listOf("丙", "丁"), CustomSourceParser.parse(rawString, config).videos.single().actors)
    }

    @Test
    fun `missing path yields empty string without exception`() {
        val raw = """{ "list": [ { "id": "1", "title": "残缺" } ] }"""

        val video = CustomSourceParser.parse(raw, config).videos.single()

        assertEquals("残缺", video.title)
        assertEquals("", video.poster)
        assertEquals("", video.summary)
        assertTrue(video.episodes.isEmpty())
    }

    @Test
    fun `data key list is recognized`() {
        val raw = """
            { "data": [ { "id": "d1", "title": "数据键" } ] }
        """.trimIndent()

        assertEquals("数据键", CustomSourceParser.parse(raw, config).videos.single().title)
    }
}
