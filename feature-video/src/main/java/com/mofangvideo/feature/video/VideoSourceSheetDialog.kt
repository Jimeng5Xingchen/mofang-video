package com.mofangvideo.feature.video

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mofangvideo.core.model.VideoSourceConfig
import com.mofangvideo.feature.video.databinding.FragmentVideoSourceSheetBinding
import com.mofangvideo.feature.video.databinding.DialogSourceUrlBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * 底部弹出的影视源管理面板：列表显示已导入源，支持启停、删除、文件导入、URL 导入。
 */
@AndroidEntryPoint
class VideoSourceSheetDialog : BottomSheetDialogFragment() {

    private var _binding: FragmentVideoSourceSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideoSourceViewModel by viewModels()

    private var urlDialogBinding: DialogSourceUrlBinding? = null

    private val openJson = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        requireContext().contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        viewModel.importFromFile(uri.toString())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoSourceSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = VideoSourceAdapter(
            onToggle = { source, enabled -> viewModel.toggle(source.id, enabled) },
            onDelete = { source -> viewModel.delete(source.id) }
        )
        binding.sourceRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.sourceRecycler.adapter = adapter

        viewModel.sources.observe(viewLifecycleOwner) { sources ->
            adapter.submitList(sources)
            binding.sourceEmpty.visibility = if (sources.isEmpty()) View.VISIBLE else View.GONE
            binding.sourceCount.text = getString(R.string.v_source_count, sources.size)
        }

        binding.importFileButton.setOnClickListener { openJson.launch(arrayOf(JSON_MIME, "*/*")) }
        binding.importUrlButton.setOnClickListener { showUrlDialog() }
        binding.closeButton.setOnClickListener { dismiss() }
    }

    private fun showUrlDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogBinding = DialogSourceUrlBinding.inflate(inflater)
        urlDialogBinding = dialogBinding

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.v_source_url_import)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.v_confirm) { _, _ ->
                val name = dialogBinding.nameEdit.text.toString().trim()
                val url = dialogBinding.urlEdit.text.toString().trim()
                val sourceName = name.ifEmpty { url }
                viewModel.importFromUrl(url, sourceName)
            }
            .setNegativeButton(R.string.v_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        urlDialogBinding = null
        _binding = null
    }

    private companion object {
        const val JSON_MIME = "application/json"
    }
}
