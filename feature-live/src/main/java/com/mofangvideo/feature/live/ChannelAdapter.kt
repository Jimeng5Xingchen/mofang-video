package com.mofangvideo.feature.live

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mofangvideo.core.model.LiveChannel
import com.mofangvideo.feature.live.databinding.ItemChannelBinding

/**
 * 直播频道列表适配器，行内含台标、频道名、分组与播放按钮。
 */
class ChannelAdapter(
    private val onPlay: (LiveChannel) -> Unit
) : ListAdapter<LiveChannel, ChannelAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemChannelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: LiveChannel) {
            binding.channelName.text = channel.name
            binding.channelGroup.text = channel.group.ifEmpty {
                binding.root.context.getString(R.string.l_title)
            }

            if (channel.logo.isNotEmpty()) {
                Glide.with(binding.channelLogo).load(channel.logo).into(binding.channelLogo)
            } else {
                binding.channelLogo.setImageResource(R.drawable.ic_live_placeholder)
            }

            binding.playButton.setOnClickListener { onPlay(channel) }
            binding.root.setOnClickListener { onPlay(channel) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<LiveChannel>() {
        override fun areItemsTheSame(oldItem: LiveChannel, newItem: LiveChannel): Boolean =
            oldItem.url == newItem.url

        override fun areContentsTheSame(oldItem: LiveChannel, newItem: LiveChannel): Boolean =
            oldItem == newItem
    }
}
