package com.mofangvideo.feature.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.mofangvideo.feature.backup.databinding.FragmentBackupBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * WebDAV 备份同步 Fragment。
 *
 * 数据本地运行，WebDAV 仅用于跨设备备份与恢复。
 */
@AndroidEntryPoint
class BackupFragment : Fragment() {

    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BackupViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.serverEdit.setText(viewModel.serverUrl)
        binding.userEdit.setText(viewModel.username)
        binding.autoBackupSwitch.isChecked = viewModel.autoBackupEnabled
        binding.autoBackupSwitch.setOnCheckedChangeListener { _, checked ->
            viewModel.setAutoBackup(checked)
        }

        binding.backupWebDavButton.setOnClickListener {
            viewModel.backupToWebDav(
                binding.serverEdit.text.toString(),
                binding.userEdit.text.toString(),
                binding.passwordEdit.text.toString()
            )
        }
        binding.restoreWebDavButton.setOnClickListener {
            viewModel.restoreFromWebDav()
        }
        binding.backupLocalButton.setOnClickListener {
            viewModel.backupToLocal()
        }
        binding.restoreLocalButton.setOnClickListener {
            viewModel.restoreFromLocal()
        }
        binding.probeButton.setOnClickListener {
            viewModel.probeWebDav(
                binding.serverEdit.text.toString(),
                binding.userEdit.text.toString(),
                binding.passwordEdit.text.toString()
            )
        }

        viewModel.status.observe(viewLifecycleOwner) { binding.statusText.text = it }
        viewModel.busy.observe(viewLifecycleOwner) { busy ->
            binding.backupProgress.visibility = if (busy) View.VISIBLE else View.GONE
        }
        viewModel.connected.observe(viewLifecycleOwner) { ok ->
            binding.connectionText.text = if (ok) "连接正常" else "未连接"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
