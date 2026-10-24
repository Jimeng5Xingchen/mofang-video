package com.mofangvideo.feature.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mofangvideo.feature.history.databinding.FragmentHistoryBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * 播放历史页：列表展示全部历史，支持单条删除与一键清空。
 */
@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = HistoryAdapter { item -> viewModel.delete(item.id) }
        binding.historyRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecycler.adapter = adapter

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_clear -> {
                    confirmClear()
                    true
                }
                else -> false
            }
        }

        viewModel.history.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.historyRecycler.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            binding.emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.load()
    }

    private fun confirmClear() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.h_history_clear)
            .setMessage(R.string.h_history_clear_confirm)
            .setNegativeButton(R.string.h_cancel, null)
            .setPositiveButton(R.string.h_confirm) { _, _ -> viewModel.clearAll() }
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
