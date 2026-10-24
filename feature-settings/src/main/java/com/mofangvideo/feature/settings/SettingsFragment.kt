package com.mofangvideo.feature.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.mofangvideo.feature.settings.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * 设置页：播放内核 / 清晰度 / 自动续播，影视源与直播源管理入口，备份入口。
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinner(
            binding.coreSpinner,
            listOf(
                "exo" to R.string.s_playback_core_exo,
                "system" to R.string.s_playback_core_system
            ),
            viewModel.playbackCore.value
        ) { value -> viewModel.setPlaybackCore(value) }

        setupSpinner(
            binding.qualitySpinner,
            listOf(
                "auto" to R.string.s_quality_auto,
                "hd" to R.string.s_quality_hd,
                "4k" to R.string.s_quality_4k
            ),
            viewModel.defaultQuality.value
        ) { value -> viewModel.setDefaultQuality(value) }

        binding.autoResumeSwitch.isChecked = viewModel.autoResume.value ?: true
        binding.autoResumeSwitch.setOnCheckedChangeListener { _, checked ->
            viewModel.setAutoResume(checked)
        }

        binding.videoSourceEntry.setOnClickListener { showVideoSources() }
        binding.liveSourceEntry.setOnClickListener { showLiveSources() }
        binding.backupEntry.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_backup)
        }
    }

    private fun showVideoSources() {
        com.mofangvideo.feature.video.VideoSourceSheetDialog()
            .show(childFragmentManager, TAG_SOURCES)
    }

    private fun showLiveSources() {
        com.mofangvideo.feature.live.LiveSourceSheetDialog()
            .show(childFragmentManager, TAG_SOURCES)
    }

    /**
     * 通用下拉选择：值-文案映射由调用方提供，选中后回调值。
     */
    private fun setupSpinner(
        spinner: Spinner,
        items: List<Pair<String, Int>>,
        selected: String?,
        onSelected: (String) -> Unit
    ) {
        val entries = items.map { getString(it.second) }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, entries)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(items.indexOfFirst { it.first == selected }.coerceAtLeast(0), false)

        spinner.setOnTouchListener { _, _ -> userPicked = true; false }
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (userPicked) {
                    userPicked = false
                    onSelected(items[position].first)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
    }

    override fun onDestroyView() {
        userPicked = false
        super.onDestroyView()
        _binding = null
    }

    private var userPicked = false

    private companion object {
        const val TAG_SOURCES = "settings_source_sheet"
    }
}
