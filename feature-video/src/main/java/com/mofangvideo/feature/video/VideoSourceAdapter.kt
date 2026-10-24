package com.mofangvideo.feature.video

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.mofangvideo.core.model.VideoSourceConfig
import com.mofangvideo.feature.video.databinding.ItemVideoSourceBinding

/**
 * 影视源列表适配器，每行显示名称、类型、接口与启停开关。
 */
class VideoSourceAdapter(
    private val onToggle: (VideoSourceConfig, Boolean) -> Unit,
    private val onDelete: (VideoSourceConfig) -> Unit
) : ListAdapter<VideoSourceConfig, VideoSourceAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemVideoSourceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemVideoSourceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(source: VideoSourceConfig) {
            binding.sourceName.text = source.name
            binding.sourceType.text = when (source.type) {
                com.mofangvideo.core.model.SourceType.CUSTOM -> "自定义源"
                com.mofangvideo.core.model.SourceType.SCRAPER -> "刮削源"
            }
            binding.sourceApi.text = source.api

            val switch: MaterialSwitch = binding.sourceSwitch
            switch.setOnCheckedChangeListener(null)
            switch.isChecked = source.enabled
            switch.setOnCheckedChangeListener { _, checked -> onToggle(source, checked) }

            binding.deleteButton.setOnClickListener { onDelete(source) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<VideoSourceConfig>() {
        override fun areItemsTheSame(oldItem: VideoSourceConfig, newItem: VideoSourceConfig): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: VideoSourceConfig, newItem: VideoSourceConfig): Boolean =
            oldItem == newItem
    }
}
