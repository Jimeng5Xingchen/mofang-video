package com.mofangvideo.tv

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.mofangvideo.feature.backup.BackupViewModel
import com.mofangvideo.feature.history.FavoriteViewModel
import com.mofangvideo.feature.history.HistoryViewModel
import com.mofangvideo.feature.live.LiveChannelViewModel
import com.mofangvideo.feature.settings.SettingsViewModel
import com.mofangvideo.feature.video.VideoSourceViewModel
import com.mofangvideo.feature.video.VideoViewModel
import com.mofangvideo.tv.leanback.ClickTarget
import com.mofangvideo.tv.leanback.TvCard
import com.mofangvideo.tv.leanback.TvCardRow
import com.mofangvideo.tv.leanback.TvRowModel

/**
 * 各分节的行数据供给器。
 *
 * 纯数据组装：从宿主 [FragmentActivity] 的 [ViewModelProvider] 取复用 feature 的 [ViewModel]，
 * 转成 [TvCardRow]。由 [TvMainActivity.browseRowsFor] 在切换分节时调用。
 */

internal fun FragmentActivity.vmVideo(): VideoViewModel =
    ViewModelProvider(this)[VideoViewModel::class.java]

internal fun FragmentActivity.vmVideoSource(): VideoSourceViewModel =
    ViewModelProvider(this)[VideoSourceViewModel::class.java]

internal fun FragmentActivity.vmLive(): LiveChannelViewModel =
    ViewModelProvider(this)[LiveChannelViewModel::class.java]

internal fun FragmentActivity.vmFavorite(): FavoriteViewModel =
    ViewModelProvider(this)[FavoriteViewModel::class.java]

internal fun FragmentActivity.vmHistory(): HistoryViewModel =
    ViewModelProvider(this)[HistoryViewModel::class.java]

internal fun FragmentActivity.vmSettings(): SettingsViewModel =
    ViewModelProvider(this)[SettingsViewModel::class.java]

internal fun FragmentActivity.vmBackup(): BackupViewModel =
    ViewModelProvider(this)[BackupViewModel::class.java]

/** 影视分节：聚合当前搜索 / 全量结果，按分类成行。 */
internal class TvVideoRows(private val host: FragmentActivity) {

    fun rows(): List<TvCardRow> {
        val videos = host.vmVideo().searchResults.value ?: emptyList()
        if (videos.isEmpty()) return emptyList()
        return TvRowModel.videoGroups(videos)
    }
}

/** 直播分节：按分组一行。 */
internal class TvLiveRows(private val host: FragmentActivity) {

    fun rows(): List<TvCardRow> {
        val channels = host.vmLive().channels.value ?: emptyList()
        if (channels.isEmpty()) return emptyList()
        return TvRowModel.liveGroups(channels)
    }
}

/** 收藏分节：影视 / 直播两行卡。 */
internal class TvFavoriteRows(private val host: FragmentActivity) {

    fun rows(): List<TvCardRow> {
        val videoFavs = host.vmFavorite().videoFavs.value.orEmpty()
        val liveFavs = host.vmFavorite().liveFavs.value.orEmpty()
        val rows = mutableListOf<TvCardRow>()
        if (videoFavs.isNotEmpty()) {
            rows += TvRowModel.singleRow(host.getString(R.string.tv_fav_video), videoFavs.map {
                TvCard(
                    key = "fav_${it.refId}",
                    title = it.title,
                    subtitle = it.refId,
                    image = it.poster,
                    onClick = ClickTarget.OpenVideo(splitRef(it.refId).first, splitRef(it.refId).second)
                )
            })
        }
        if (liveFavs.isNotEmpty()) {
            rows += TvRowModel.singleRow(host.getString(R.string.tv_fav_live), liveFavs.map {
                TvCard(
                    key = "fav_${it.refId}",
                    title = it.title,
                    subtitle = it.refId,
                    image = it.poster,
                    onClick = liveClickTarget(it.refId)
                )
            })
        }
        return rows
    }

    private fun liveClickTarget(refId: String): ClickTarget {
        val channel = host.vmLive().channels.value?.firstOrNull { it.id == refId }
        return channel?.let { ClickTarget.OpenLive(it) }
            ?: ClickTarget.OpenVideo(splitRef(refId).first, splitRef(refId).second)
    }

    private fun splitRef(refId: String): Pair<String, String> {
        val idx = refId.lastIndexOf('_')
        return if (idx > 0) refId.substring(0, idx) to refId.substring(idx + 1)
        else "" to refId
    }
}

/** 历史分节：按时间倒序单行卡，含观看进度百分比。 */
internal class TvHistoryRows(private val host: FragmentActivity) {

    fun rows(): List<TvCardRow> {
        val history = (host.vmHistory().history.value ?: emptyList())
            .sortedByDescending { it.playedAt }
        if (history.isEmpty()) return emptyList()
        val cards = history.map { h ->
            val percent = if (h.durationMs > 0) (h.progressMs * 100 / h.durationMs).toInt() else 0
            TvCard(
                key = "hist_${h.id}",
                title = h.title,
                subtitle = host.getString(R.string.tv_history_progress, percent),
                image = "",
                resumePercent = percent,
                onClick = ClickTarget.OpenVideo(splitRef(h.refId).first, splitRef(h.refId).second)
            )
        }
        return listOf(TvRowModel.singleRow(host.getString(R.string.tv_history_title), cards))
    }

    private fun splitRef(refId: String): Pair<String, String> {
        val idx = refId.lastIndexOf('_')
        return if (idx > 0) refId.substring(0, idx) to refId.substring(idx + 1)
        else "" to refId
    }
}

/** 设置分节：每项一个可焦点卡，中键循环切换取值。 */
internal class TvSettingsRows(private val host: FragmentActivity) {

    fun rows(): List<TvCardRow> {
        val vm = host.vmSettings()
        val cards = listOf(
            TvCard(
                key = "set_core",
                title = host.getString(R.string.tv_settings_playback_core),
                subtitle = vm.playbackCore.value ?: "exo",
                image = ""
            ),
            TvCard(
                key = "set_quality",
                title = host.getString(R.string.tv_settings_default_quality),
                subtitle = vm.defaultQuality.value ?: "auto",
                image = ""
            ),
            TvCard(
                key = "set_autoresume",
                title = host.getString(R.string.tv_settings_auto_resume),
                subtitle = if (vm.autoResume.value == true)
                    host.getString(R.string.tv_settings_on) else host.getString(R.string.tv_settings_off),
                image = ""
            ),
            TvCard(
                key = "set_theme",
                title = host.getString(R.string.tv_settings_theme_mode),
                subtitle = themeLabel(vm.themeMode.value),
                image = ""
            ),
            TvCard(
                key = "set_update",
                title = host.getString(R.string.tv_settings_check_update),
                subtitle = host.getString(R.string.tv_settings_check_update_sub),
                image = "",
                onClick = ClickTarget.OpenUpdate
            )
        )
        return listOf(TvRowModel.singleRow(host.getString(R.string.tv_tab_settings), cards))
    }

    private fun themeLabel(mode: String?): String = when (mode) {
        "light" -> host.getString(R.string.tv_settings_theme_light)
        "dark" -> host.getString(R.string.tv_settings_theme_dark)
        else -> host.getString(R.string.tv_settings_theme_system)
    }
}

/** 备份分节：WebDAV 配置与备份 / 恢复操作入口。 */
internal class TvBackupRows(private val host: FragmentActivity) {

    fun rows(): List<TvCardRow> {
        val vm = host.vmBackup()
        val cards = listOf(
            TvCard(
                key = "backup_webdav",
                title = host.getString(R.string.tv_backup_webdav_url),
                subtitle = vm.serverUrl.ifBlank { host.getString(R.string.tv_cancel) },
                image = ""
            ),
            TvCard(
                key = "backup_backup",
                title = host.getString(R.string.tv_backup_backup),
                subtitle = if (vm.autoBackupEnabled)
                    host.getString(R.string.tv_settings_on) else host.getString(R.string.tv_settings_off),
                image = ""
            ),
            TvCard(
                key = "backup_restore",
                title = host.getString(R.string.tv_backup_restore),
                subtitle = "",
                image = ""
            ),
            TvCard(
                key = "backup_local",
                title = host.getString(R.string.tv_backup_local_backup),
                subtitle = "",
                image = ""
            )
        )
        return listOf(TvRowModel.singleRow(host.getString(R.string.tv_tab_backup), cards))
    }
}
