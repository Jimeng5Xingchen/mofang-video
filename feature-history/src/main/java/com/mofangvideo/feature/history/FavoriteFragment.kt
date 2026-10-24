package com.mofangvideo.feature.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.mofangvideo.core.model.FavType
import com.mofangvideo.feature.history.databinding.FragmentFavoriteBinding
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

/**
 * 收藏页：影视 / 直播两个 Tab，支持取消收藏。
 */
@AndroidEntryPoint
class FavoriteFragment : Fragment() {

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoriteViewModel by viewModels()
    private lateinit var adapter: FavoriteAdapter

    private var currentType: FavType = FavType.VIDEO

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FavoriteAdapter { item -> viewModel.remove(item.type, item.refId) }
        binding.favoriteRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.favoriteRecycler.adapter = adapter

        setupTabs()

        viewModel.videoFavs.observe(viewLifecycleOwner) { list ->
            if (currentType == FavType.VIDEO) show(list)
        }
        viewModel.liveFavs.observe(viewLifecycleOwner) { list ->
            if (currentType == FavType.LIVE) show(list)
        }
        viewModel.loadAll()
    }

    private fun setupTabs() {
        binding.tabs.removeAllTabs()
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.h_favorite_video))
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.h_favorite_live))
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentType = if (tab?.position == 1) FavType.LIVE else FavType.VIDEO
                viewModel.loadAll()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
    }

    private fun show(list: List<com.mofangvideo.core.model.Favorite>) {
        adapter.submitList(list)
        binding.favoriteRecycler.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.emptyText.setText(
            if (currentType == FavType.LIVE) R.string.h_favorite_live_empty else R.string.h_favorite_video_empty
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadAll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
