package com.mofangvideo.feature.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.mofangvideo.feature.backup.databinding.FragmentBackupBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * WebDAV 备份同步 Fragment。
 *
 * 数据本地运行，WebDAV 仅用于跨设备备份与恢复。功能：
 * - WebDAV 地址/账号/密码/备份路径配置
 * - 检测连接、保存、保存并立即备份
 * - 定时备份开关（WorkManager 周期任务）
 * - 本地备份/恢复 + 上次本地备份信息
 * - 远程备份文件列表 + 指定文件恢复
 */
@AndroidEntryPoint
class BackupFragment : Fragment() {

    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BackupViewModel by viewModels()

    private lateinit var remoteAdapter: RemoteBackupAdapter

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

        remoteAdapter = RemoteBackupAdapter { file ->
            binding.statusText.text = getString(R.string.backup_selected_remote, file.name)
        }
        binding.remoteFilesList.layoutManager = LinearLayoutManager(requireContext())
        binding.remoteFilesList.adapter = remoteAdapter

        binding.serverEdit.setText(viewModel.serverUrl)
        binding.userEdit.setText(viewModel.username)
        binding.pathEdit.setText(viewModel.backupPath)
        binding.autoBackupSwitch.isChecked = viewModel.autoBackupEnabled

        binding.autoBackupSwitch.setOnCheckedChangeListener { _, checked ->
            viewModel.setAutoBackup(checked)
        }

        binding.testConnectionButton.setOnClickListener {
            viewModel.testConnection(
                binding.serverEdit.text.toString(),
                binding.userEdit.text.toString(),
                binding.passwordEdit.text.toString(),
                binding.pathEdit.text.toString()
            )
        }

        binding.saveButton.setOnClickListener {
            viewModel.save(
                binding.serverEdit.text.toString(),
                binding.userEdit.text.toString(),
                binding.passwordEdit.text.toString(),
                binding.pathEdit.text.toString()
            )
        }

        binding.saveAndBackupButton.setOnClickListener {
            viewModel.saveAndBackup(
                binding.serverEdit.text.toString(),
                binding.userEdit.text.toString(),
                binding.passwordEdit.text.toString(),
                binding.pathEdit.text.toString()
            )
        }

        binding.refreshRemoteButton.setOnClickListener {
            viewModel.listRemoteFiles()
        }

        binding.restoreWebDavButton.setOnClickListener {
            val selected = remoteAdapter.selectedFile()
            if (selected != null) viewModel.restoreFromRemoteFile(selected.name)
            else viewModel.restoreFromWebDav()
        }

        binding.backupLocalButton.setOnClickListener { viewModel.backupToLocal() }
        binding.restoreLocalButton.setOnClickListener { viewModel.restoreFromLocal() }

        viewModel.status.observe(viewLifecycleOwner) { binding.statusText.text = it }
        viewModel.busy.observe(viewLifecycleOwner) { busy ->
            binding.backupProgress.visibility = if (busy) View.VISIBLE else View.GONE
        }
        viewModel.connected.observe(viewLifecycleOwner) { ok ->
            binding.connectionText.text =
                if (ok == null) getString(R.string.backup_disconnected)
                else if (ok) getString(R.string.backup_connected) else getString(R.string.backup_disconnected)
        }
        viewModel.localInfo.observe(viewLifecycleOwner) { info ->
            if (info == null) {
                binding.localInfoText.setText(R.string.backup_no_local)
            } else {
                val time = android.text.format.DateFormat
                    .getTimeFormat(requireContext())
                    .format(java.util.Date(info.modifiedAt))
                binding.localInfoText.text = getString(
                    R.string.backup_local_info_format,
                    time,
                    RemoteBackupAdapter.formatSize(info.sizeBytes)
                )
            }
        }
        viewModel.remoteFiles.observe(viewLifecycleOwner) { files ->
            remoteAdapter.submitList(files)
            binding.remoteFilesList.visibility = if (files.isEmpty()) View.GONE else View.VISIBLE
            binding.remoteEmptyText.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
