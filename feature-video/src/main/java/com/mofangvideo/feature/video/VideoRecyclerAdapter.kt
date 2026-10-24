package com.mofangvideo.feature.video

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mofangvideo.core.model.Video
import com.mofangvideo.feature.video.databinding.ItemVideoCardBinding

/**
 * 影视卡片网格适配器。
 *
 * 使用 [ListAdapter] + [DiffUtil] 做增量刷新，海报经 Glide 加载并带占位色。
 */
class VideoRecyclerAdapter(
    private val onClick: (Video) -> Unit
) : ListAdapter<Video, VideoRecyclerAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemVideoCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemVideoCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(video: Video) {
            binding.titleText.text = video.title
            if (video.year.isNotEmpty()) {
                binding.yearText.visibility = android.view.View.VISIBLE
                binding.yearText.text = video.year
            } else {
                binding.yearText.visibility = android.view.View.GONE
            }

            val poster = video.poster
            val request = Glide.with(binding.posterImage)
            if (poster.isNotEmpty()) {
                request.load(poster).placeholder(R.drawable.ic_movie_placeholder)
            } else {
                request.clear(binding.posterImage)
                binding.posterImage.setImageResource(R.drawable.ic_movie_placeholder)
            }
            binding.root.setOnClickListener { onClick(video) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Video>() {
        override fun areItemsTheSame(oldItem: Video, newItem: Video): Boolean =
            oldItem.id == newItem.id && oldItem.sourceId == newItem.sourceId

        override fun areContentsTheSame(oldItem: Video, newItem: Video): Boolean = oldItem == newItem
    }
}
