package com.mofangvideo.feature.video

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.mofangvideo.core.model.FavType
import com.mofangvideo.core.model.Episode
import com.mofangvideo.core.player.PlayerFacade
import com.mofangvideo.core.repo.FavoriteRepository
import com.mofangvideo.core.repo.HistoryRepository
import com.mofangvideo.feature.video.databinding.ActivityVideoPlayBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 点播播放页：ExoPlayer 播放 m3u8，支持收藏与续播落库。
 */
@AndroidEntryPoint
class VideoPlayActivity : AppCompatActivity() {

    @Inject
    lateinit var favoriteRepo: FavoriteRepository

    @Inject
    lateinit var historyRepo: HistoryRepository

    private lateinit var binding: ActivityVideoPlayBinding
    private val playerFacade: PlayerFacade by lazy { PlayerFacade(this) }

    private var url: String = ""
    private var title: String = ""
    private var episodeName: String = ""
    private var refId: String = ""
    private var favoriteRef: String = ""
    private var favoriteTitle: String = ""
    private var resumeMs: Long = 0L
    private var favorited = false

    private var handle: com.mofangvideo.core.player.PlayerHandle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        url = intent.getStringExtra(EXTRA_URL).orEmpty()
        title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        episodeName = intent.getStringExtra(EXTRA_EPISODE).orEmpty()
        refId = intent.getStringExtra(EXTRA_REF_ID).orEmpty()
        favoriteRef = intent.getStringExtra(EXTRA_FAVORITE_REF).orEmpty()
        favoriteTitle = title
        resumeMs = intent.getLongExtra(EXTRA_RESUME_MS, 0L)

        binding.toolbar.title = listOf(title, episodeName).filter { it.isNotEmpty() }.joinToString(" · ")
        binding.toolbar.setNavigationIcon(com.mofangvideo.feature.video.R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.toolbar.menu.clear()
        if (favoriteRef.isNotEmpty()) {
            binding.toolbar.inflateMenu(R.menu.menu_play)
            val item = binding.toolbar.menu.findItem(R.id.action_favorite)
            item.setIcon(
                if (favorited) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
            )
            item.setOnMenuItemClickListener {
                toggleFavorite()
                true
            }
        }

        startPlayback()
        refreshFavoriteState()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun startPlayback() {
        val episode = Episode(
            id = "0",
            name = episodeName,
            url = url,
            quality = episodeName
        )
        handle = playerFacade.buildPoint(episode, resumeMs)
        binding.playerView.player = handle?.player
        binding.playProgress.show()
        handle?.player?.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    binding.playProgress.hide()
                }
            }
        })
    }

    private fun refreshFavoriteState() {
        if (favoriteRef.isEmpty()) return
        lifecycleScope.launch {
            favorited = favoriteRepo.isFavorited(FavType.VIDEO, favoriteRef)
            binding.toolbar.menu.findItem(R.id.action_favorite)?.setIcon(
                if (favorited) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
            )
        }
    }

    private fun toggleFavorite() {
        if (favoriteRef.isEmpty()) return
        lifecycleScope.launch {
            if (favorited) {
                favoriteRepo.remove(FavType.VIDEO, favoriteRef)
            } else {
                favoriteRepo.add(FavType.VIDEO, favoriteRef, favoriteTitle)
            }
            favorited = favoriteRepo.isFavorited(FavType.VIDEO, favoriteRef)
            binding.toolbar.menu.findItem(R.id.action_favorite)?.setIcon(
                if (favorited) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
            )
            Snackbar.make(
                binding.root,
                if (favorited) R.string.v_detail_favorited else R.string.v_detail_favorite,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun onPause() {
        super.onPause()
        saveProgress()
    }

    /** 暂停时写入播放历史，支持下次续播。 */
    private fun saveProgress() {
        val player = handle?.player ?: return
        val position = player.currentPosition
        val duration = player.duration
        if (duration <= 0) return
        lifecycleScope.launch {
            historyRepo.record(
                type = FavType.VIDEO,
                refId = refId,
                title = title,
                progressMs = position,
                durationMs = duration
            )
        }
    }

    override fun onDestroy() {
        handle?.let { playerFacade.release(it) }
        handle = null
        binding.playerView.player = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_EPISODE = "extra_episode"
        const val EXTRA_REF_ID = "extra_ref_id"
        const val EXTRA_FAVORITE_REF = "extra_favorite_ref"
        const val EXTRA_POSTER = "extra_poster"
        const val EXTRA_RESUME_MS = "extra_resume_ms"
    }
}
