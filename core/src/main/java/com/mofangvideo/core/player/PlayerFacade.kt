package com.mofangvideo.core.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mofangvideo.core.model.Episode
import com.mofangvideo.core.model.LiveChannel

/**
 * 播放器句柄，封装 ExoPlayer 实例、当前媒体项与错误重试监听器。
 *
 * 调用方通过 [PlayerFacade.release] 释放，确保监听器被正确移除。
 */
data class PlayerHandle(
    val player: ExoPlayer,
    val mediaItem: MediaItem?,
    val listener: Player.Listener,
    val isLive: Boolean = false
)

/**
 * 播放器门面。
 *
 * 职责：构建 ExoPlayer 实例、m3u8 播放、断网/播放错误自动重试、清晰度切换、续播定位。
 * 调用方在页面销毁时必须调用 [release] 释放资源。
 */
class PlayerFacade(private val context: Context) {

    /**
     * 构建点播播放器。
     *
     * @param episode 待播集，含播放地址与清晰度
     * @param resumeMs 续播进度（毫秒），0 表示从头播放
     */
    fun buildPoint(episode: Episode, resumeMs: Long = 0L): PlayerHandle {
        val player = ExoPlayer.Builder(context).build()
        val item = MediaItem.fromUri(episode.url)
        player.setMediaItem(item)
        val listener = RetryListener(player)
        player.addListener(listener)
        player.prepare()
        if (resumeMs > 0) player.seekTo(resumeMs)
        player.playWhenReady = true
        return PlayerHandle(player, item, listener, isLive = false)
    }

    /**
     * 构建直播播放器。
     */
    fun buildLive(channel: LiveChannel): PlayerHandle {
        val player = ExoPlayer.Builder(context).build()
        val item = MediaItem.fromUri(channel.url)
        player.setMediaItem(item)
        val listener = RetryListener(player)
        player.addListener(listener)
        player.prepare()
        player.playWhenReady = true
        return PlayerHandle(player, item, listener, isLive = true)
    }

    /**
     * 切换清晰度（仅点播），保留当前播放进度。
     */
    fun switchQuality(handle: PlayerHandle, episode: Episode) {
        val player = handle.player
        val progress = player.currentPosition
        player.setMediaItem(MediaItem.fromUri(episode.url))
        player.seekTo(progress)
        player.prepare()
        player.playWhenReady = true
    }

    /**
     * 释放播放器资源，先移除监听器再 release。
     */
    fun release(handle: PlayerHandle) {
        handle.player.removeListener(handle.listener)
        handle.player.release()
    }

    /**
     * 播放错误自动重试，最多 [MAX_RETRIES] 次，每次从头重新缓冲。
     */
    private class RetryListener(private val player: ExoPlayer) : Player.Listener {

        private var retryCount = 0

        override fun onPlayerError(error: PlaybackException) {
            if (retryCount >= MAX_RETRIES) return
            retryCount++
            player.seekTo(0)
            player.prepare()
            player.play()
        }
    }

    companion object {
        const val MAX_RETRIES = 3
    }
}
