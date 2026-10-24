package com.mofangvideo.tv.leanback

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.mofangvideo.tv.R

/**
 * 顶栏分节 Tab 单卡 Presenter：仅文本（影视 / 直播 / 收藏 / 历史 / 设置 / 备份）。
 */
class TvTabPresenter : Presenter() {

    class ViewHolder(view: View) : Presenter.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = View.inflate(parent.context, R.layout.item_tv_tab, null)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        val tab = item as? TvTab ?: return
        (viewHolder.view as? TextView)?.text = tab.label
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) = Unit
}

/**
 * 顶栏 Tab 数据。
 */
data class TvTab(
    val section: String,
    val label: String,
    val selected: Boolean = false
)
