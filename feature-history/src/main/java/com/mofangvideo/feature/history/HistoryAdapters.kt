package com.mofangvideo.feature.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mofangvideo.core.model.Favorite
import com.mofangvideo.core.model.FavType
import com.mofangvideo.core.model.PlayHistory
import com.mofangvideo.feature.history.databinding.ItemFavoriteBinding

/**
 * 收藏列表适配器：海报、标题、类型标签、播放进度（若有）、删除按钮。
 */
class FavoriteAdapter(
    private val onRemove: (Favorite) -> Unit
) : ListAdapter<Favorite, FavoriteAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemFavoriteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Favorite) {
            binding.titleText.text = item.title
            binding.metaText.text = binding.root.context.getString(
                when (item.type) {
                    FavType.VIDEO -> R.string.h_favorite_video
                    FavType.LIVE -> R.string.h_favorite_live
                }
            )
            binding.progressText.visibility = android.view.View.GONE
            binding.deleteButton.setOnClickListener { onRemove(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Favorite>() {
        override fun areItemsTheSame(oldItem: Favorite, newItem: Favorite): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Favorite, newItem: Favorite): Boolean =
            oldItem == newItem
    }
}

/**
 * 播放历史列表适配器，带续播进度条文字。
 */
class HistoryAdapter(
    private val onDelete: (PlayHistory) -> Unit
) : ListAdapter<PlayHistory, HistoryAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemFavoriteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlayHistory) {
            binding.titleText.text = item.title
            binding.metaText.text = binding.root.context.getString(
                when (item.type) {
                    FavType.VIDEO -> R.string.h_favorite_video
                    FavType.LIVE -> R.string.h_favorite_live
                }
            ) + "  ·  " + formatTime(item.playedAt)

            if (item.durationMs > 0 && item.progressMs > 0) {
                binding.progressText.visibility = android.view.View.VISIBLE
                val percent = (item.progressMs * 100 / item.durationMs).toInt().coerceIn(0, 100)
                binding.progressText.text = binding.root.context.getString(
                    R.string.h_history_progress_format,
                    percent
                )
            } else {
                binding.progressText.visibility = android.view.View.GONE
            }

            binding.deleteButton.setOnClickListener { onDelete(item) }
        }

        private fun formatTime(millis: Long): String {
            val date = java.util.Date(millis)
            return java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(date)
        }
    }

    private object Diff : DiffUtil.ItemCallback<PlayHistory>() {
        override fun areItemsTheSame(oldItem: PlayHistory, newItem: PlayHistory): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PlayHistory, newItem: PlayHistory): Boolean =
            oldItem == newItem
    }
}
