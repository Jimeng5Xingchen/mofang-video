package com.mofangvideo.feature.live

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.mofangvideo.core.model.LiveSourceConfig
import com.mofangvideo.feature.live.databinding.ItemLiveSourceBinding

/**
 * 直播源列表适配器，行内显示名称、频道数、开关与删除。
 */
class LiveSourceAdapter(
    private val onToggle: (LiveSourceConfig, Boolean) -> Unit,
    private val onDelete: (LiveSourceConfig) -> Unit
) : ListAdapter<LiveSourceConfig, LiveSourceAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemLiveSourceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemLiveSourceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(source: LiveSourceConfig) {
            binding.sourceName.text = source.name
            binding.sourceMeta.text = binding.root.context.getString(
                R.string.l_source_count,
                source.channelCount
            )
            binding.sourceUrl.text = source.url

            val switch: MaterialSwitch = binding.sourceSwitch
            switch.setOnCheckedChangeListener(null)
            switch.isChecked = source.enabled
            switch.setOnCheckedChangeListener { _, checked -> onToggle(source, checked) }

            binding.deleteButton.setOnClickListener { onDelete(source) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<LiveSourceConfig>() {
        override fun areItemsTheSame(oldItem: LiveSourceConfig, newItem: LiveSourceConfig): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: LiveSourceConfig, newItem: LiveSourceConfig): Boolean =
            oldItem == newItem
    }
}
