package com.mofangvideo.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpgParserTest {

    private val xmltv = """
        <?xml version="1.0" encoding="UTF-8"?>
        <tv>
          <channel id="CH001">
            <display-name>频道A</display-name>
            <programme start="20240101080000 +0800" stop="20240101090000 +0800">
              <title>早间新闻</title>
            </programme>
            <programme start="20240101090000 +0800" stop="20240101100000 +0800">
              <title>天气预报</title>
            </programme>
          </channel>
          <channel id="CH002">
            <display-name>频道B</display-name>
            <programme start="2024-01-01T12:00:00" stop="2024-01-01T13:00:00">
              <title>午间剧场</title>
            </programme>
          </channel>
        </tv>
    """.trimIndent()

    @Test
    fun `parses programmes of target channel`() {
        val result = EpgParser.parse("CH001", xmltv)

        assertEquals(2, result!!.items.size)
        assertEquals("早间新闻", result.items[0].title)
        assertEquals("天气预报", result.items[1].title)
        assertTrue(result.items[0].start < result.items[0].end)
    }

    @Test
    fun `channel id is carried into result`() {
        assertEquals("CH002", EpgParser.parse("CH002", xmltv)!!.channelId)
    }

    @Test
    fun `time with timezone offset parses as utc`() {
        // 2024-01-01 08:00:00 +0800 == 2024-01-01 00:00:00 UTC
        assertEquals(1704067200000L, EpgParser.parseTime("20240101080000 +0800"))
    }

    @Test
    fun `iso style time without offset parses as utc`() {
        // 2024-01-01 12:00:00 UTC
        assertEquals(1704110400000L, EpgParser.parseTime("2024-01-01T12:00:00"))
    }

    @Test
    fun `short or invalid time returns zero`() {
        assertEquals(0L, EpgParser.parseTime(""))
        assertEquals(0L, EpgParser.parseTime("20240101"))
        assertEquals(0L, EpgParser.parseTime("abc"))
    }

    @Test
    fun `unknown channel returns null`() {
        assertNull(EpgParser.parse("NO_SUCH", xmltv))
    }

    @Test
    fun `empty tvg id returns null`() {
        assertNull(EpgParser.parse("", xmltv))
    }

    @Test
    fun `channel without programmes returns null`() {
        val raw = """
            <tv>
              <channel id="EMPTY"><display-name>空频道</display-name></channel>
            </tv>
        """.trimIndent()

        assertNull(EpgParser.parse("EMPTY", raw))
    }

    @Test
    fun `malformed xml returns null instead of throwing`() {
        assertNull(EpgParser.parse("CH001", "<<<not xml###"))
    }

    @Test
    fun `title with markup and whitespace is trimmed`() {
        val raw = """
            <tv>
              <channel id="T1">
                <programme start="20240101080000 +0000" stop="20240101090000 +0000">
                  <title lang="zh">  带空格标题  </title>
                </programme>
              </channel>
            </tv>
        """.trimIndent()

        assertEquals("带空格标题", EpgParser.parse("T1", raw)!!.items.single().title)
    }
}
