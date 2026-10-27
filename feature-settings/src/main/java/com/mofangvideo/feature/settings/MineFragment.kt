package com.mofangvideo.feature.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.mofangvideo.feature.history.FavoriteViewModel
import com.mofangvideo.feature.history.HistoryViewModel
import com.mofangvideo.feature.settings.databinding.FragmentMineBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * 「我的」聚合页：用户信息、收藏 / 历史入口（含计数）、设置入口。
 */
@AndroidEntryPoint
class MineFragment : Fragment() {

    private var _binding: FragmentMineBinding? = null
    private val binding get() = _binding!!

    private val favoriteViewModel: FavoriteViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.favoriteEntry.setOnClickListener {
            findNavController().navigate(R.id.action_mine_to_favorites)
        }
        binding.historyEntry.setOnClickListener {
            findNavController().navigate(R.id.action_mine_to_history)
        }
        binding.settingsEntry.setOnClickListener {
            findNavController().navigate(R.id.action_mine_to_settings)
        }

        favoriteViewModel.favorites.observe(viewLifecycleOwner) { favorites ->
            binding.favoriteCount.text = getString(R.string.mine_favorites_count, favorites?.size ?: 0)
        }
        historyViewModel.history.observe(viewLifecycleOwner) { history ->
            binding.historyCount.text = getString(R.string.mine_history_count, history?.size ?: 0)
        }

        favoriteViewModel.loadAll()
        historyViewModel.load()
    }

    override fun onResume() {
        super.onResume()
        favoriteViewModel.loadAll()
        historyViewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
