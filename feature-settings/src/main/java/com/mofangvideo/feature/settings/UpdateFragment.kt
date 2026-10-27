package com.mofangvideo.feature.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.mofangvideo.core.update.UpdateChannel
import com.mofangvideo.feature.settings.databinding.FragmentUpdateBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * 检查更新页：渠道选择、检查、自动下载、安装引导。
 */
@AndroidEntryPoint
class UpdateFragment : Fragment() {

    private var _binding: FragmentUpdateBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UpdateViewModel by viewModels()

    private var readyFile: java.io.File? = null

    private val currentVersion: String
        get() = try {
            requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
                .versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.currentVersion.text = getString(R.string.s_current_version, currentVersion)
        binding.channelGroup.check(R.id.channel_stable)

        binding.channelGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val channel = if (checkedId == R.id.channel_beta) UpdateChannel.BETA else UpdateChannel.STABLE
            viewModel.setChannel(channel)
        }

        binding.checkButton.setOnClickListener {
            viewModel.check(currentVersion)
        }

        binding.retryButton.setOnClickListener {
            viewModel.retry(currentVersion)
        }

        binding.installButton.setOnClickListener { installApk() }

        viewModel.channel.observe(viewLifecycleOwner) { channel ->
            val id = if (channel == UpdateChannel.BETA) R.id.channel_beta else R.id.channel_stable
            if (binding.channelGroup.checkedButtonId != id) binding.channelGroup.check(id)
        }

        viewModel.state.observe(viewLifecycleOwner) { render(it) }
    }

    private fun render(state: UpdateUiState) {
        binding.checkButton.visibility = View.GONE
        binding.retryButton.visibility = View.GONE
        binding.installButton.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.progressText.visibility = View.GONE
        binding.notesLabel.visibility = View.GONE
        binding.notesText.visibility = View.GONE

        when (state) {
            is UpdateUiState.Idle -> {
                binding.statusText.setText(R.string.u_check_now)
                binding.checkButton.visibility = View.VISIBLE
            }
            is UpdateUiState.Checking -> {
                binding.progressBar.isIndeterminate = true
                binding.progressBar.visibility = View.VISIBLE
                binding.statusText.setText(R.string.u_checking)
            }
            is UpdateUiState.Latest -> {
                binding.statusText.text = getString(R.string.u_latest, state.version)
                binding.checkButton.visibility = View.VISIBLE
            }
            is UpdateUiState.Available -> {
                binding.statusText.text = getString(R.string.u_new_version, state.update.versionName)
                binding.progressBar.isIndeterminate = false
                binding.progressBar.visibility = View.VISIBLE
                binding.progressText.visibility = View.VISIBLE
                binding.progressText.text =
                    getString(R.string.u_size, state.update.apkSizeBytes / 1048576.0)
                binding.notesLabel.visibility = View.VISIBLE
                binding.notesText.visibility = View.VISIBLE
                binding.notesText.text = state.update.releaseNotes.ifBlank { "—" }
            }
            is UpdateUiState.Downloading -> {
                binding.progressBar.isIndeterminate = false
                binding.progressBar.progress = state.percent
                binding.progressBar.visibility = View.VISIBLE
                binding.progressText.visibility = View.VISIBLE
                binding.progressText.text = getString(R.string.u_downloading, state.percent)
            }
            is UpdateUiState.ReadyToInstall -> {
                readyFile = state.file
                binding.statusText.setText(R.string.u_download_done)
                binding.installButton.visibility = View.VISIBLE
            }
            is UpdateUiState.Failed -> {
                binding.statusText.text = getString(R.string.u_failed, state.message)
                binding.retryButton.visibility = View.VISIBLE
            }
        }
    }

    private fun installApk() {
        val file = readyFile ?: return
        val ok = viewModel.install(requireContext(), file)
        if (!ok) {
            Toast.makeText(requireContext(), R.string.u_need_install_permission, Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:${requireContext().packageName}"))
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
