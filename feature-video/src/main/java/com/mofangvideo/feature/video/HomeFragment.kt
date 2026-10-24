package com.mofangvideo.feature.video

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.mofangvideo.feature.video.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * 影视首页：搜索入口 + 影视源管理入口 + 影视卡片网格。
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val searchViewModel: VideoViewModel by viewModels()
    private val sourceViewModel: VideoSourceViewModel by viewModels()

    private lateinit var adapter: VideoRecyclerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupSearch()
        setupSourceEntry()
        observe()
    }

    private fun setupRecycler() {
        adapter = VideoRecyclerAdapter { video ->
            findNavController().navigate(
                R.id.action_home_to_detail,
                bundleOf(ARG_SOURCE_ID to video.sourceId, ARG_VIDEO_ID to video.id)
            )
        }
        binding.videoRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.videoRecycler.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchEdit.doAfterTextChanged { text ->
            if (text.isNullOrBlank()) searchViewModel.clear()
        }
        binding.searchEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchViewModel.search(binding.searchEdit.text.toString())
                hideKeyboard()
                true
            } else {
                false
            }
        }
    }

    private fun setupSourceEntry() {
        binding.sourceButton.setOnClickListener {
            VideoSourceSheetDialog().show(childFragmentManager, TAG_SOURCE_SHEET)
        }
    }

    private fun observe() {
        searchViewModel.searchResults.observe(viewLifecycleOwner) { videos ->
            adapter.submitList(videos)
            binding.emptyText.visibility = if (videos.isEmpty()) View.VISIBLE else View.GONE
            if (videos.isEmpty()) {
                binding.emptyText.setText(
                    if (searchViewModel.error.value != null) R.string.v_search_no_result
                    else R.string.v_search_empty
                )
            }
        }
        searchViewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.searchProgress.visibility = if (loading) View.VISIBLE else View.GONE
        }
        sourceViewModel.message.observe(viewLifecycleOwner) { message ->
            message ?: return@observe
            com.google.android.material.snackbar.Snackbar
                .make(binding.root, message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                .show()
            sourceViewModel.consumeMessage()
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.searchEdit.windowToken, 0)
    }

    companion object {
        private const val TAG_SOURCE_SHEET = "video_source_sheet"
        const val ARG_SOURCE_ID = "sourceId"
        const val ARG_VIDEO_ID = "videoId"
    }
}
