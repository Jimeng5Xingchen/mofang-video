package com.mofangvideo.feature.live

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.mofangvideo.core.model.FavType
import com.mofangvideo.core.model.LiveChannel
import com.mofangvideo.core.player.PlayerFacade
import com.mofangvideo.core.repo.FavoriteRepository
import com.mofangvideo.core.repo.HistoryRepository
import com.mofangvideo.feature.live.databinding.ActivityLivePlayBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 直播播放页：ExoPlayer 播放直播流，支持收藏与播放历史。
 */
@AndroidEntryPoint
class LivePlayActivity : AppCompatActivity() {

    @Inject
    lateinit var favoriteRepo: FavoriteRepository

    @Inject
    lateinit var historyRepo: HistoryRepository

    private lateinit var binding: ActivityLivePlayBinding
    private val playerFacade: PlayerFacade by lazy { PlayerFacade(this) }

    private var channelId: String = ""
    private var channelUrl: String = ""
    private var channelName: String = ""
    private var favorited = false

    private var handle: com.mofangvideo.core.player.PlayerHandle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLivePlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        channelId = intent.getStringExtra(EXTRA_CHANNEL_ID).orEmpty()
        channelUrl = intent.getStringExtra(EXTRA_CHANNEL_URL).orEmpty()
        channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME).orEmpty()
        val logo = intent.getStringExtra(EXTRA_CHANNEL_LOGO).orEmpty()

        binding.toolbar.title = channelName
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.inflateMenu(R.menu.menu_live_play)
        binding.toolbar.setOnMenuItemClickListener { item ->
            item.itemId == R.id.action_favorite && toggleFavorite()
        }

        startPlayback(channelId, channelUrl, channelName, logo)
        refreshFavoriteState()
    }

    private fun startPlayback(id: String, url: String, name: String, logo: String) {
        val channel = LiveChannel(
            id = id,
            sourceId = "",
            name = name,
            logo = logo,
            url = url
        )
        handle = playerFacade.buildLive(channel)
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
        lifecycleScope.launch {
            favorited = favoriteRepo.isFavorited(FavType.LIVE, channelId)
            binding.toolbar.menu.findItem(R.id.action_favorite)?.setIcon(
                if (favorited) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
            )
        }
    }

    private fun toggleFavorite(): Boolean {
        lifecycleScope.launch {
            if (favorited) {
                favoriteRepo.remove(FavType.LIVE, channelId)
            } else {
                favoriteRepo.add(FavType.LIVE, channelId, channelName)
            }
            favorited = favoriteRepo.isFavorited(FavType.LIVE, channelId)
            binding.toolbar.menu.findItem(R.id.action_favorite)?.setIcon(
                if (favorited) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
            )
            Snackbar.make(
                binding.root,
                if (favorited) R.string.l_favorited else R.string.l_favorite,
                Snackbar.LENGTH_SHORT
            ).show()
        }
        return true
    }

    override fun onStart() {
        super.onStart()
        recordLiveHistory()
    }

    private fun recordLiveHistory() {
        if (channelId.isEmpty()) return
        lifecycleScope.launch {
            historyRepo.record(
                type = FavType.LIVE,
                refId = channelId,
                title = channelName,
                progressMs = 0L,
                durationMs = 0L
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
        const val EXTRA_CHANNEL_ID = "extra_channel_id"
        const val EXTRA_CHANNEL_URL = "extra_channel_url"
        const val EXTRA_CHANNEL_NAME = "extra_channel_name"
        const val EXTRA_CHANNEL_LOGO = "extra_channel_logo"
    }
}
