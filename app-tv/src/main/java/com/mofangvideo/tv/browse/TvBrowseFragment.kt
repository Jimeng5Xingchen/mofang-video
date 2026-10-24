package com.mofangvideo.tv.browse

import android.os.Bundle
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import com.mofangvideo.tv.leanback.TvBrowsePresenter
import com.mofangvideo.tv.leanback.TvCard
import com.mofangvideo.tv.leanback.TvCardRow

/**
 * Leanback 行式浏览片段（AndroidX 版）：分类标题 + 横向卡片条。
 *
 * 继承 [BrowseSupportFragment]（AndroidX Fragment，适配 Hilt / AndroidX TV 壳），
 * 行模型由 [TvBrowsePresenter] 生成；系统头禁用（顶栏分节由主壳管理），
 * D-pad 上下切行、左右切卡片、中键选中交由 Leanback 焦点引擎处理。
 *
 * @param section 当前分节标识（行标题）。
 * @param initialRows 首次创建时展示的行数据。
 */
class TvBrowseFragment(
    private val section: String,
    initialRows: List<TvCardRow> = emptyList()
) : BrowseSupportFragment() {

    var rows: List<TvCardRow> = initialRows
        private set

    var onCardClick: ((TvCard) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHeadersState(HEADERS_DISABLED)
        setOnItemViewClickedListener(object : OnItemViewClickedListener {
            override fun onItemClicked(
                presentViewHolder: Presenter.ViewHolder?,
                item: Any?,
                rowViewHolder: RowPresenter.ViewHolder?,
                row: Row?
            ) {
                (item as? TvCard)?.let { onCardClick?.invoke(it) }
            }
        })
        applyRows()
    }

    /** 外部刷新行数据。 */
    fun setRows(rows: List<TvCardRow>) {
        this.rows = rows
        applyRows()
    }

    private fun applyRows() {
        setAdapter(TvBrowsePresenter.rowsAdapter(rows))
    }
}
