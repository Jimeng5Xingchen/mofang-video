package com.mofangvideo.tv.leanback

import com.mofangvideo.core.model.EpgItem
import com.mofangvideo.core.model.LiveChannel
import com.mofangvideo.core.model.Video

/**
 * TV 卡片数据，承载单张卡片的展示信息与选中动作目标。
 *
 * 纯 Kotlin，零 Android 依赖，行模型构建可在 JVM 单测。
 */
data class TvCard(
    val key: String,
    val title: String,
    val subtitle: String,
    val image: String,
    val resumePercent: Int = 0,
    val onClick: ClickTarget = ClickTarget.None
)

/**
 * 卡片点按（中键）后的动作目标。
 */
sealed class ClickTarget {
    class OpenVideo(val sourceId: String, val videoId: String) : ClickTarget()
    class OpenLive(val channel: LiveChannel) : ClickTarget()
    object None : ClickTarget()
}

/**
 * 一行卡片：标题 + 卡片列表。
 * 交给 Leanback `VerticalGridView` 渲染为"分类标题 + 横向卡片条"。
 */
data class TvCardRow(
    val header: String,
    val cards: List<TvCard>
) {
    val isEmpty: Boolean get() = cards.isEmpty()
}

/**
 * 行模型构建器：把各类业务数据转成 [TvCardRow] 列表。
 *
 * 纯逻辑，零 Android 依赖，JVM 可单测。
 */
object TvRowModel {

    /** 影视：按分类分组，每组一行。空分类不建行。 */
    fun videoGroups(videos: List<Video>): List<TvCardRow> {
        val grouped = videos.groupBy { it.category.ifBlank { "全部" } }
        return grouped.map { (group, items) ->
            TvCardRow(
                header = group,
                cards = items.map { v ->
                    TvCard(
                        key = "${v.sourceId}_${v.id}",
                        title = v.title,
                        subtitle = buildVideoSubtitle(v),
                        image = v.poster,
                        onClick = ClickTarget.OpenVideo(v.sourceId, v.id)
                    )
                }
            )
        }.filterNot { it.isEmpty }
    }

    private fun buildVideoSubtitle(v: Video): String {
        val parts = mutableListOf(v.year, v.category)
        if (v.actors.isNotEmpty()) parts += "主演 ${v.actors.first()}"
        return parts.filter { it.isNotBlank() }.joinToString(" · ")
    }

    /** 直播：按分组一行。 */
    fun liveGroups(channels: List<LiveChannel>): List<TvCardRow> {
        val grouped = channels.groupBy { it.group.ifBlank { "默认" } }
        return grouped.map { (group, items) ->
            TvCardRow(
                header = group,
                cards = items.map { c ->
                    TvCard(
                        key = c.id,
                        title = c.name,
                        subtitle = c.group,
                        image = c.logo,
                        onClick = ClickTarget.OpenLive(c)
                    )
                }
            )
        }.filterNot { it.isEmpty }
    }

    /** 单行：收藏 / 历史统一渲染为卡片行。 */
    fun singleRow(header: String, cards: List<TvCard>): TvCardRow =
        TvCardRow(header, cards)
}
