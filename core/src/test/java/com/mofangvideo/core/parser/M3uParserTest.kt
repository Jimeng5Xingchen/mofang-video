package com.mofangvideo.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    @Test
    fun `standard format parses name and url`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,央视一套
            http://example.com/cctv1.m3u8
        """.trimIndent()

        val result = M3uParser.parse(content)

        assertEquals(1, result.channels.size)
        assertEquals(0, result.skippedCount)
        val ch = result.channels[0]
        assertEquals("央视一套", ch.name)
        assertEquals("http://example.com/cctv1.m3u8", ch.url)
        assertEquals("默认", ch.group)
        assertEquals("", ch.tvgId)
        assertEquals("", ch.logo)
    }

    @Test
    fun `extended tvg attributes are extracted`() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-id="CH001" tvg-name="频道A" tvg-logo="http://logo/a.png" tvg-group="体育",频道A
            http://example.com/a.m3u8
        """.trimIndent()

        val result = M3uParser.parse(content)

        val ch = result.channels.single()
        assertEquals("频道A", ch.name)
        assertEquals("CH001", ch.tvgId)
        assertEquals("http://logo/a.png", ch.logo)
        assertEquals("体育", ch.group)
    }

    @Test
    fun `missing group falls back to default`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,新闻频道
            http://example.com/news.m3u8
        """.trimIndent()

        assertEquals("默认", M3uParser.parse(content).channels.single().group)
    }

    @Test
    fun `invalid comment lines are counted as skipped`() {
        val content = """
            #EXTM3U
            #EXTVLCOPT:http-user-agent=test
            #EXTINF:-1,频道A
            http://example.com/a.m3u8
            garbage-line
        """.trimIndent()

        val result = M3uParser.parse(content)

        assertEquals(1, result.channels.size)
        assertEquals(2, result.skippedCount)
    }

    @Test
    fun `url without extinf still creates channel`() {
        val content = """
            #EXTM3U
            http://example.com/raw.m3u8
        """.trimIndent()

        val result = M3uParser.parse(content)

        assertEquals(1, result.channels.size)
        assertEquals("http://example.com/raw.m3u8", result.channels[0].url)
        assertEquals("", result.channels[0].name)
    }

    @Test
    fun `empty name falls back to tvg-name`() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-id="C1" tvg-name="备用名",
            http://example.com/c.m3u8
        """.trimIndent()

        assertEquals("备用名", M3uParser.parse(content).channels.single().name)
    }

    @Test
    fun `multiple channels get sequential ids`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,频道A
            http://example.com/a.m3u8
            #EXTINF:-1 tvg-id="CH2",频道B
            http://example.com/b.m3u8
        """.trimIndent()

        val result = M3uParser.parse(content)

        assertEquals(2, result.channels.size)
        assertTrue(result.channels[0].id != result.channels[1].id)
        assertEquals("CH2", result.channels[1].tvgId)
    }

    @Test
    fun `parsing is idempotent`() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-id="CH001" tvg-group="体育",频道A
            http://example.com/a.m3u8
        """.trimIndent()

        val first = M3uParser.parse(content)
        val second = M3uParser.parse(content)

        assertEquals(first, second)
    }

    @Test
    fun `empty content yields empty result`() {
        val result = M3uParser.parse("")
        assertTrue(result.channels.isEmpty())
        assertEquals(0, result.skippedCount)
    }
}
