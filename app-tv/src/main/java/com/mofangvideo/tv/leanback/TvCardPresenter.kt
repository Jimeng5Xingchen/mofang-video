package com.mofangvideo.tv.leanback

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.mofangvideo.tv.R

/**
 * 单卡片 Presenter：海报 + 标题 + 副标题 + 续播进度条。
 *
 * 焦点态由 Leanback `CardView` 自动放大 / 描边，此处处理进度条与海报。
 */
class TvCardPresenter : Presenter() {

    class ViewHolder(view: View) : Presenter.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = View.inflate(parent.context, R.layout.item_tv_card, null)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        val card = item as? TvCard ?: return
        val root = viewHolder.view
        root.tag = card

        val poster = root.findViewById<ImageView>(R.id.ivPoster)
        val title = root.findViewById<TextView>(R.id.tvTitle)
        val subtitle = root.findViewById<TextView>(R.id.tvSubtitle)
        val progress = root.findViewById<ProgressBar>(R.id.pbProgress)
        val emptyBadge = root.findViewById<TextView>(R.id.tvEmpty)

        Glide.with(poster.context)
            .load(card.image)
            .centerCrop()
            .placeholder(R.drawable.bg_tv_card)
            .into(poster)

        title.text = card.title
        if (card.subtitle.isNotBlank()) {
            subtitle.visibility = View.VISIBLE
            subtitle.text = card.subtitle
        } else {
            subtitle.visibility = View.GONE
        }

        if (card.resumePercent in 1..99) {
            progress.visibility = View.VISIBLE
            progress.progress = card.resumePercent
        } else {
            progress.visibility = View.GONE
        }

        if (card.title.isEmpty()) {
            emptyBadge.visibility = View.VISIBLE
            emptyBadge.text = "暂无内容"
        } else {
            emptyBadge.visibility = View.GONE
        }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        viewHolder.view.findViewById<ImageView>(R.id.ivPoster)?.let { Glide.with(it.context).clear(it) }
    }
}
