package com.mofangvideo.tv.leanback

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ObjectAdapter

/**
 * 把 [TvCardRow] 列表组织成 Leanback 行模型（[BrowseSupportFragment] 消费）。
 *
 * 每行 = 分类标题（[HeaderItem]）+ 横向卡片条（[ArrayObjectAdapter] 承载 [TvCard]），
 * 渲染为"分类标题 + 横向卡片条"。D-pad 上下切行、左右切卡片、中键选中由 Leanback 焦点引擎处理。
 */
object TvBrowsePresenter {

    /**
     * 构建行数据源：每个分类一个 [ListRow]，卡片统一用 [TvCardPresenter] 渲染。
     * 返回供 [androidx.leanback.app.BrowseSupportFragment.setAdapter] 使用的 [ObjectAdapter]。
     */
    fun rowsAdapter(rows: List<TvCardRow>): ObjectAdapter {
        val rowsAdapter = ArrayObjectAdapter()
        rows.forEach { row ->
            val header = HeaderItem(row.header)
            header.contentDescription = row.header
            val items = ArrayObjectAdapter(TvCardPresenter())
            items.addAll(row.cards.size, row.cards)
            rowsAdapter.add(ListRow(header, items))
        }
        return rowsAdapter
    }
}
