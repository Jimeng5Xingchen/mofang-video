package com.mofangvideo.tv

import com.mofangvideo.core.model.LiveChannel
import com.mofangvideo.core.model.Video
import com.mofangvideo.tv.leanback.ClickTarget
import com.mofangvideo.tv.leanback.TvCard
import com.mofangvideo.tv.leanback.TvRowModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 行模型构建纯逻辑单测（JVM，零 Android 依赖）。
 */
class TvRowModelTest {

    private fun video(id: String, title: String, category: String = "", year: String = "2024"): Video =
        Video(
            id = id,
            sourceId = "src",
            title = title,
            poster = "http://x/$id.jpg",
            year = year,
            category = category
        )

    private fun channel(id: String, name: String, group: String = ""): LiveChannel =
        LiveChannel(
            id = id,
            sourceId = "src",
            name = name,
            group = group,
            url = "http://live/$id.m3u8"
        )

    @Test
    fun `videoGroups groups by category and filters empty`() {
        val rows = TvRowModel.videoGroups(
            listOf(
                video("1", "A", "电影"),
                video("2", "B", "电视剧"),
                video("3", "C", "电影"),
                video("4", "D") // blank category -> "全部"
            )
        )
        val headers = rows.map { it.header }
        assertEquals(3, rows.size)
        assertTrue(headers.containsAll(listOf("电影", "电视剧", "全部")))
        val movieRow = rows.first { it.header == "电影" }
        assertEquals(2, movieRow.cards.size)
        // key = sourceId_videoId
        assertEquals("src_1", movieRow.cards.first { it.title == "A" }.key)
    }

    @Test
    fun `videoGroups empty input yields no rows`() {
        assertTrue(TvRowModel.videoGroups(emptyList()).isEmpty())
    }

    @Test
    fun `video subtitle joins year category and first actor`() {
        val v = Video(
            id = "1",
            sourceId = "src",
            title = "A",
            poster = "",
            year = "2023",
            actors = listOf("甲", "乙"),
            category = "电影"
        )
        val rows = TvRowModel.videoGroups(listOf(v))
        val subtitle = rows.single().cards.single().subtitle
        assertTrue(subtitle.contains("2023"))
        assertTrue(subtitle.contains("电影"))
        assertTrue(subtitle.contains("甲"))
    }

    @Test
    fun `liveGroups groups by group and opens live channel`() {
        val rows = TvRowModel.liveGroups(
            listOf(
                channel("c1", "央视1", "央视"),
                channel("c2", "卫视A", "卫视"),
                channel("c3", "央视2", "央视")
            )
        )
        assertEquals(2, rows.size)
        val cctvRow = rows.first { it.header == "央视" }
        assertEquals(2, cctvRow.cards.size)
        val target = cctvRow.cards.first { it.title == "央视1" }.onClick
        assertTrue(target is ClickTarget.OpenLive)
        assertEquals("c1", (target as ClickTarget.OpenLive).channel.id)
    }

    @Test
    fun `singleRow returns one row with given header`() {
        val cards = listOf(TvCard(key = "k", title = "t", subtitle = "s", image = "", onClick = ClickTarget.OpenVideo("src", "v")))
        val row = TvRowModel.singleRow("标题", cards)
        assertEquals("标题", row.header)
        assertEquals(1, row.cards.size)
        assertTrue(!row.isEmpty)
    }

    @Test
    fun `card isEmpty reflects card list`() {
        assertTrue(TvRowModel.singleRow("h", emptyList()).isEmpty)
    }
}
