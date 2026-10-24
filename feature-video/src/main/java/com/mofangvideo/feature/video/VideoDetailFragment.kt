package com.mofangvideo.feature.video

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.mofangvideo.core.model.Video
import com.mofangvideo.feature.video.databinding.FragmentVideoDetailBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * 影视详情：海报、元信息、简介、选集、播放与收藏。
 */
@AndroidEntryPoint
class VideoDetailFragment : Fragment() {

    private var _binding: FragmentVideoDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideoDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.favoriteButton.setOnClickListener { viewModel.toggleFavorite() }

        val sourceId = arguments?.getString(HomeFragment.ARG_SOURCE_ID).orEmpty()
        val videoId = arguments?.getString(HomeFragment.ARG_VIDEO_ID).orEmpty()

        viewModel.video.observe(viewLifecycleOwner) { video ->
            if (video == null) {
                binding.summaryText.text = getString(R.string.v_detail_no_episode)
                return@observe
            }
            render(video)
        }
        viewModel.favorited.observe(viewLifecycleOwner) { favorited ->
            binding.favoriteButton.setText(if (favorited) R.string.v_detail_favorited else R.string.v_detail_favorite)
            binding.favoriteButton.icon = requireContext().getDrawable(
                if (favorited) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
            )
        }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.detailProgress.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.load(sourceId, videoId)
    }

    private fun render(video: Video) {
        binding.toolbar.title = video.title
        binding.titleText.text = video.title

        val meta = buildList {
            if (video.year.isNotEmpty()) add(video.year)
            if (video.category.isNotEmpty()) add(video.category)
            if (video.actors.isNotEmpty()) add(video.actors.joinToString(" / "))
        }.joinToString("  ·  ")
        binding.metaText.text = meta

        if (video.summary.isNotEmpty()) {
            binding.summaryLabel.visibility = View.VISIBLE
            binding.summaryText.visibility = View.VISIBLE
            binding.summaryText.text = video.summary
        }

        if (video.poster.isNotEmpty()) {
            Glide.with(this).load(video.poster).into(binding.posterImage)
        }

        binding.episodeGroup.removeAllViews()
        if (video.episodes.isEmpty()) {
            binding.playButton.isEnabled = false
            binding.summaryLabel.visibility = View.VISIBLE
            binding.summaryText.visibility = View.VISIBLE
            binding.summaryText.text = getString(R.string.v_detail_no_episode)
        } else {
            binding.episodesLabel.visibility = View.VISIBLE
            binding.episodeGroup.visibility = View.VISIBLE
            video.episodes.forEachIndexed { index, episode ->
                val chip = Chip(requireContext())
                chip.text = episode.name.ifBlank { getString(R.string.v_episode_index, index + 1) }
                chip.isCheckable = index == 0
                chip.isChecked = index == 0
                chip.tag = episode
                chip.setOnClickListener { startPlay(video, index) }
                binding.episodeGroup.addView(chip)
            }
            binding.playButton.setOnClickListener { startPlay(video, 0) }
        }
    }

    private fun startPlay(video: Video, episodeIndex: Int) {
        val episode = video.episodes.getOrNull(episodeIndex) ?: return
        viewModel.resumeMs { resumeMs ->
            startActivity(
                Intent(requireContext(), VideoPlayActivity::class.java).apply {
                    putExtra(VideoPlayActivity.EXTRA_URL, episode.url)
                    putExtra(VideoPlayActivity.EXTRA_TITLE, video.title)
                    putExtra(VideoPlayActivity.EXTRA_EPISODE, episode.name)
                    putExtra(VideoPlayActivity.EXTRA_REF_ID, videoRef(video.sourceId, video.id))
                    putExtra(VideoPlayActivity.EXTRA_FAVORITE_REF, videoRef(video.sourceId, video.id))
                    putExtra(VideoPlayActivity.EXTRA_POSTER, video.poster)
                    putExtra(VideoPlayActivity.EXTRA_RESUME_MS, resumeMs)
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
