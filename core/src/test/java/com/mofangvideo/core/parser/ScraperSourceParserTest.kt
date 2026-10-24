package com.mofangvideo.core.parser

import com.mofangvideo.core.model.SourceType
import com.mofangvideo.core.model.VideoSourceConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScraperSourceParserTest {

    private val config = VideoSourceConfig(
        id = "src1",
        name = "刮削源",
        type = SourceType.SCRAPER,
        api = "https://example.com/api.php"
    )

    @Test
    fun `parses standard ac list response with default field mapping`() {
        val raw = """
            {
              "list": [
                {
                  "vod_id": 101,
                  "vod_name": "流浪地球",
                  "vod_pic": "http://img/earth.jpg",
                  "vod_year": "2019",
                  "actor": "吴京,屈楚萧",
                  "vod_content": "太阳即将毁灭",
                  "type_name": "科幻",
                  "vod_play_url": "正片::http://stream/earth.m3u8"
                }
              ]
            }
        """.trimIndent()

        val result = ScraperSourceParser.parse(raw, config)

        assertEquals(1, result.videos.size)
        val video = result.videos[0]
        assertEquals("101", video.id)
        assertEquals("流浪地球", video.title)
        assertEquals("http://img/earth.jpg", video.poster)
        assertEquals("2019", video.year)
        assertEquals(listOf("吴京", "屈楚萧"), video.actors)
        assertEquals("太阳即将毁灭", video.summary)
        assertEquals("科幻", video.category)
        assertEquals(1, video.episodes.size)
        assertEquals("正片", video.episodes[0].name)
        assertEquals("http://stream/earth.m3u8", video.episodes[0].url)
    }

    @Test
    fun `custom field mapping overrides defaults`() {
        val config = config.copy(
            fields = mapOf(
                "title" to "name",
                "poster" to "cover",
                "year" to "year",
                "playUrl" to "urls"
            )
        )
        val raw = """
            {
              "videos": [
                { "name": "测试片", "cover": "http://img/c.jpg", "year": "2020", "urls": "HD::http://s/1.m3u8" }
              ]
            }
        """.trimIndent()

        val result = ScraperSourceParser.parse(raw, config)

        val video = result.videos.single()
        assertEquals("测试片", video.title)
        assertEquals("http://img/c.jpg", video.poster)
        assertEquals("2020", video.year)
        assertEquals("http://s/1.m3u8", video.episodes.single().url)
    }

    @Test
    fun `episodes split by hash and parse name url pairs`() {
        val raw = """
            {
              "list": [
                { "vod_id": 1, "vod_name": "剧", "vod_play_url": "第1集::http://s/1.m3u8#第2集::http://s/2.m3u8" }
              ]
            }
        """.trimIndent()

        val episodes = ScraperSourceParser.parse(raw, config).videos.single().episodes

        assertEquals(2, episodes.size)
        assertEquals("第1集", episodes[0].name)
        assertEquals("http://s/1.m3u8", episodes[0].url)
        assertEquals("第2集", episodes[1].name)
        assertEquals("http://s/2.m3u8", episodes[1].url)
    }

    @Test
    fun `bare url without name separator still becomes episode`() {
        val raw = """
            {
              "list": [
                { "vod_id": 1, "vod_name": "片", "vod_play_url": "http://s/movie.mp4" }
              ]
            }
        """.trimIndent()

        val episodes = ScraperSourceParser.parse(raw, config).videos.single().episodes

        assertEquals(1, episodes.size)
        assertEquals("http://s/movie.mp4", episodes[0].url)
        assertEquals("http://s/movie.mp4", episodes[0].name)
    }

    @Test
    fun `missing fields degrade to empty values`() {
        val raw = """
            { "list": [ { "vod_id": 5, "vod_name": "裸片" } ] }
        """.trimIndent()

        val video = ScraperSourceParser.parse(raw, config).videos.single()

        assertEquals("裸片", video.title)
        assertEquals("", video.poster)
        assertEquals("", video.summary)
        assertTrue(video.actors.isEmpty())
        assertTrue(video.episodes.isEmpty())
    }

    @Test
    fun `non object entries are skipped without breaking parse`() {
        val raw = """
            { "list": [ "bad-entry", { "vod_id": 1, "vod_name": "好片" }, 42 ] }
        """.trimIndent()

        val result = ScraperSourceParser.parse(raw, config)

        assertEquals(1, result.videos.size)
        assertEquals("好片", result.videos[0].title)
    }

    @Test
    fun `empty list yields empty result`() {
        val result = ScraperSourceParser.parse("""{ "list": [] }""", config)
        assertTrue(result.videos.isEmpty())
    }

    @Test
    fun `data key list is recognized`() {
        val raw = """
            { "data": [ { "vod_id": 9, "vod_name": "数据片" } ] }
        """.trimIndent()

        assertEquals("数据片", ScraperSourceParser.parse(raw, config).videos.single().title)
    }
}
