package com.mofangvideo.feature.live

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mofangvideo.core.model.LiveChannel
import com.mofangvideo.feature.live.databinding.DialogLiveSourceUrlBinding
import com.mofangvideo.feature.live.databinding.FragmentLiveBinding
import com.mofangvideo.feature.live.databinding.FragmentLiveSourceSheetBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * 直播首页：分组 Chips + 频道列表，点击进入播放。
 */
@AndroidEntryPoint
class LiveFragment : Fragment() {

    private var _binding: FragmentLiveBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LiveChannelViewModel by viewModels()
    private val sourceViewModel: LiveSourceViewModel by viewModels()

    private lateinit var channelAdapter: ChannelAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        channelAdapter = ChannelAdapter { channel -> play(channel) }
        binding.liveRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.liveRecycler.adapter = channelAdapter

        binding.sourceButton.setOnClickListener {
            LiveSourceSheetDialog().show(childFragmentManager, TAG_SOURCE_SHEET)
        }

        viewModel.groups.observe(viewLifecycleOwner) { groups ->
            binding.groupChips.removeAllViews()
            groups.forEach { group ->
                val chip = Chip(requireContext())
                chip.text = group
                chip.isCheckable = true
                chip.isChecked = group == viewModel.selectedGroup.value
                chip.setOnClickListener { viewModel.selectGroup(group) }
                binding.groupChips.addView(chip)
            }
        }

        viewModel.channels.observe(viewLifecycleOwner) { channels ->
            channelAdapter.submitList(channels)
            binding.liveRecycler.visibility = if (channels.isEmpty()) View.GONE else View.VISIBLE
            binding.emptyText.visibility = if (channels.isEmpty()) View.VISIBLE else View.GONE
        }

        sourceViewModel.message.observe(viewLifecycleOwner) { message ->
            message ?: return@observe
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            sourceViewModel.consumeMessage()
            viewModel.loadGroups()
            viewModel.loadChannels()
        }

        viewModel.loadGroups()
        viewModel.loadChannels()
    }

    private fun play(channel: LiveChannel) {
        startActivity(
            Intent(requireContext(), LivePlayActivity::class.java).apply {
                putExtra(LivePlayActivity.EXTRA_CHANNEL_ID, channel.id)
                putExtra(LivePlayActivity.EXTRA_CHANNEL_URL, channel.url)
                putExtra(LivePlayActivity.EXTRA_CHANNEL_NAME, channel.name)
                putExtra(LivePlayActivity.EXTRA_CHANNEL_LOGO, channel.logo)
            }
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadChannels()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAG_SOURCE_SHEET = "live_source_sheet"
    }
}

/**
 * 底部弹出的直播源管理面板：M3U 文件导入、URL 导入、启停与删除。
 */
@AndroidEntryPoint
class LiveSourceSheetDialog : BottomSheetDialogFragment() {

    private var _binding: FragmentLiveSourceSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LiveSourceViewModel by viewModels()

    private val openM3u = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        viewModel.importFromFile(uri.toString())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveSourceSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = LiveSourceAdapter(
            onToggle = { source, enabled -> viewModel.toggle(source.id, enabled) },
            onDelete = { source -> viewModel.delete(source.id) }
        )
        binding.sourceRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.sourceRecycler.adapter = adapter

        viewModel.sources.observe(viewLifecycleOwner) { sources ->
            adapter.submitList(sources)
            binding.sourceEmpty.visibility = if (sources.isEmpty()) View.VISIBLE else View.GONE
            binding.sourceCount.text = getString(R.string.l_source_count, sources.size)
        }

        binding.importFileButton.setOnClickListener { openM3u.launch(arrayOf("*/*")) }
        binding.importUrlButton.setOnClickListener { showUrlDialog() }
        binding.closeButton.setOnClickListener { dismiss() }
    }

    private fun showUrlDialog() {
        val dialogBinding = DialogLiveSourceUrlBinding.inflate(LayoutInflater.from(requireContext()))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.l_source_add)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.l_confirm) { _, _ ->
                val name = dialogBinding.nameEdit.text.toString().trim()
                val url = dialogBinding.urlEdit.text.toString().trim()
                viewModel.importFromUrl(url, name.ifEmpty { nameFromUrl(url) })
            }
            .setNegativeButton(R.string.l_cancel, null)
            .show()
    }

    private fun nameFromUrl(url: String): String =
        url.substringAfterLast('/').substringBeforeLast('.').ifEmpty { "直播源" }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
