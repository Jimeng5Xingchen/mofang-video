package com.mofangvideo.tv.play

import androidx.activity.ComponentActivity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.mofangvideo.core.model.Episode
import com.mofangvideo.core.model.FavType
import com.mofangvideo.core.model.LiveChannel
import com.mofangvideo.core.player.PlayerFacade
import com.mofangvideo.core.player.PlayerHandle
import com.mofangvideo.core.repo.HistoryRepository
import com.mofangvideo.feature.video.VideoDetailViewModel
import com.mofangvideo.tv.R
import com.mofangvideo.tv.focus.TvPlayAction
import com.mofangvideo.tv.focus.TvPlayKeyMapper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * TV 统一播放页：点播 / 直播共用，media3 [PlayerView] + 遥控 D-pad 映射。
 *
 * - 点播：传入 source / video，经 [VideoDetailViewModel] 取剧集并支持续播与清晰度切换。
 * - 直播：传入 [LiveChannel]，上下键切台。
 * 销毁 / 切走必须 [PlayerFacade.release]。
 */
@AndroidEntryPoint
class TvPlayActivity : ComponentActivity() {

    @Inject lateinit var historyRepo: HistoryRepository

    private val playerFacade: PlayerFacade by lazy { PlayerFacade(this) }
    private var handle: PlayerHandle? = null

    private lateinit var playerView: PlayerView
    private lateinit var tvStatus: TextView

    private var isLive = false
    private var sourceId = ""
    private var videoId = ""
    private var refId = ""
    private var title = ""
    private var episodes: List<Episode> = emptyList()
    private var currentEpisode: Episode? = null
    private var liveChannel: LiveChannel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_play)
        playerView = findViewById(R.id.playerView)
        tvStatus = findViewById(R.id.tvStatus)
        wireControls()
        startFromIntent()
    }

    private fun wireControls() {
        // 播放页焦点固定在 playerView，遥控 D-pad 全量映射（见 dispatchKeyEvent），无需触屏控件。
    }

    private fun startFromIntent() {
        isLive = intent.getBooleanExtra(EXTRA_IS_LIVE, false)
        if (isLive) {
            val channel = intent.extrasLiveChannel()
            if (channel == null) {
                finish()
                return
            }
            liveChannel = channel
            startLive(channel)
        } else {
            sourceId = intent.getStringExtra(EXTRA_SOURCE_ID).orEmpty()
            videoId = intent.getStringExtra(EXTRA_VIDEO_ID).orEmpty()
            refId = "${sourceId}_$videoId"
            startPoint()
        }
    }

    private fun startLive(channel: LiveChannel) {
        title = channel.name
        tvStatus.text = channel.name
        handle?.let { playerFacade.release(it) }
        handle = playerFacade.buildLive(channel)
        bindPlayer()
    }

    private fun startPoint() {
        tvStatus.text = "正在载入…"
        val detailViewModel = ViewModelProvider(this)[VideoDetailViewModel::class.java]
        detailViewModel.load(sourceId, videoId)
        detailViewModel.video.observe(this) { video ->
            video ?: return@observe
            title = video.title
            episodes = video.episodes
            detailViewModel.resumeMs { resume ->
                val first = episodes.firstOrNull() ?: return@resumeMs
                playEpisode(first, resume)
            }
        }
    }

    private fun playEpisode(episode: Episode, resumeMs: Long = 0L) {
        currentEpisode = episode
        tvStatus.text = listOf(title, episode.name).filter { it.isNotBlank() }.joinToString(" · ")
        handle?.let { playerFacade.release(it) }
        handle = playerFacade.buildPoint(episode, resumeMs)
        bindPlayer()
    }

    private fun bindPlayer() {
        val h = handle ?: return
        playerView.player = h.player
        playerView.useController = false
        h.player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> if (tvStatus.text.isBlank()) tvStatus.text = "正在播放"
                    Player.STATE_BUFFERING -> tvStatus.text = "缓冲中…"
                    Player.STATE_ENDED -> tvStatus.text = "结束"
                    else -> if (tvStatus.text.isBlank()) tvStatus.text = "正在加载…"
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                tvStatus.setText(R.string.tv_play_network_error)
            }
        })
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val action = TvPlayKeyMapper.map(event.keyCode)
            if (action != TvPlayAction.NONE) {
                applyPlayAction(action)
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun applyPlayAction(action: TvPlayAction) {
        val player = handle?.player ?: return
        when (action) {
            TvPlayAction.PLAY_PAUSE -> if (player.isPlaying) player.pause() else player.play()
            TvPlayAction.SEEK_BACK -> seek(-SEEK_STEP_MS)
            TvPlayAction.SEEK_FWD -> seek(SEEK_STEP_MS)
            TvPlayAction.PREVIOUS -> stepEpisode(-1)
            TvPlayAction.NEXT -> stepEpisode(1)
            TvPlayAction.FULLSCREEN -> toggleFullscreen()
            TvPlayAction.NONE -> Unit
        }
    }

    private fun seek(deltaMs: Long) {
        if (isLive) return
        val player = handle?.player ?: return
        player.seekTo((player.currentPosition + deltaMs).coerceIn(0L, player.duration))
    }

    private fun stepEpisode(direction: Int) {
        if (isLive) return
        val current = episodes.indexOfFirst { it == currentEpisode }
        val next = (current + direction).let { if (it in episodes.indices) it else current }
        episodes.getOrNull(next)?.let { playEpisode(it, 0L) }
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (isFullscreen) {
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        } else 0
    }

    private var isFullscreen = false

    override fun onPause() {
        super.onPause()
        saveProgress()
    }

    private fun saveProgress() {
        val h = handle ?: return
        if (h.isLive) return
        val progress = h.player.currentPosition
        val duration = h.player.duration
        val ref = refId
        val t = title
        if (ref.isEmpty()) return
        lifecycleScope.launch {
            historyRepo.record(FavType.VIDEO, ref, t, progress, duration)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handle?.let { playerFacade.release(it) }
        handle = null
    }

    private fun Intent.extrasLiveChannel(): LiveChannel? {
        val id = getStringExtra(EXTRA_CHANNEL_ID).orEmpty()
        val url = getStringExtra(EXTRA_CHANNEL_URL).orEmpty()
        if (id.isEmpty() && url.isEmpty()) return null
        return LiveChannel(
            id = id,
            sourceId = "",
            name = getStringExtra(EXTRA_CHANNEL_NAME).orEmpty(),
            group = "",
            logo = getStringExtra(EXTRA_CHANNEL_LOGO).orEmpty(),
            url = url
        )
    }

    companion object {
        private const val SEEK_STEP_MS = 15_000L
        const val EXTRA_SOURCE_ID = "sourceId"
        const val EXTRA_VIDEO_ID = "videoId"
        const val EXTRA_CHANNEL_ID = "channelId"
        const val EXTRA_CHANNEL_URL = "channelUrl"
        const val EXTRA_CHANNEL_NAME = "channelName"
        const val EXTRA_CHANNEL_LOGO = "channelLogo"
        const val EXTRA_IS_LIVE = "isLive"
    }
}
