package com.mofangvideo.feature.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.mofangvideo.feature.live.LiveSourceSheetDialog
import com.mofangvideo.feature.settings.databinding.FragmentSettingsBinding
import com.mofangvideo.feature.video.VideoSourceSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * 设置页：二级分类 —— 播放设置 / 通用 / 源管理 / 数据 / 检查更新 / 关于。
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    private var userPicked = false

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

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

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

        setupSpinner(
            binding.themeSpinner,
            listOf(
                "system" to R.string.s_theme_system,
                "light" to R.string.s_theme_light,
                "dark" to R.string.s_theme_dark
            ),
            viewModel.themeMode.value
        ) { value ->
            viewModel.setThemeMode(value)
            AppCompatDelegate.setDefaultNightMode(nightModeOf(value))
        }

        binding.clearCacheEntry.setOnClickListener { clearCache() }

        binding.videoSourceEntry.setOnClickListener {
            VideoSourceSheetDialog().show(childFragmentManager, TAG_SOURCES)
        }
        binding.liveSourceEntry.setOnClickListener {
            LiveSourceSheetDialog().show(childFragmentManager, TAG_SOURCES)
        }
        binding.backupEntry.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_backup)
        }

        binding.checkUpdateEntry.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_update)
        }

        binding.versionText.text = getString(
            R.string.s_version,
            currentVersion()
        )
    }

    private fun currentVersion(): String = try {
        requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0)
            .versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }

    private fun nightModeOf(mode: String): Int = when (mode) {
        "light" -> AppCompatDelegate.MODE_NIGHT_NO
        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    private fun clearCache() {
        MainScope().launch(Dispatchers.IO) {
            Glide.get(requireContext()).clearDiskCache()
        }
        Glide.get(requireContext()).clearMemory()
        Toast.makeText(requireContext(), R.string.s_cache_cleared, Toast.LENGTH_SHORT).show()
    }

    /**
     * 通用下拉选择：值-文案映射由调用方提供，仅在用户主动选择后回调。
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
        val initial = items.indexOfFirst { it.first == selected }.coerceAtLeast(0)
        spinner.setSelection(initial, false)

        spinner.setOnTouchListener { _, _ -> userPicked = true; false }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (userPicked) {
                    userPicked = false
                    onSelected(items[position].first)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    override fun onDestroyView() {
        userPicked = false
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAG_SOURCES = "settings_source_sheet"
    }
}
