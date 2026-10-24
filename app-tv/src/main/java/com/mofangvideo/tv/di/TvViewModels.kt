package com.mofangvideo.tv.di

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mofangvideo.feature.backup.BackupViewModel
import com.mofangvideo.feature.history.FavoriteViewModel
import com.mofangvideo.feature.history.HistoryViewModel
import com.mofangvideo.feature.live.LiveChannelViewModel
import com.mofangvideo.feature.settings.SettingsViewModel
import com.mofangvideo.feature.video.VideoDetailViewModel
import com.mofangvideo.feature.video.VideoSourceViewModel
import com.mofangvideo.feature.video.VideoViewModel

/**
 * TV 端 ViewModel 获取。
 *
 * TV 壳用 Leanback `android.app.Fragment`（非 AndroidX），无法走 `@AndroidEntryPoint`
 * 片段注入；改为从 Hilt 化宿主 Activity 的 [ViewModelStore] 直接取 [ViewModel]。
 */
object TvViewModels {

    private fun <T : ViewModel> get(activity: FragmentActivity, type: Class<T>): T =
        ViewModelProvider(activity).get(type)

    fun video(activity: FragmentActivity): VideoViewModel =
        get(activity, VideoViewModel::class.java)

    fun videoSource(activity: FragmentActivity): VideoSourceViewModel =
        get(activity, VideoSourceViewModel::class.java)

    fun videoDetail(activity: FragmentActivity): VideoDetailViewModel =
        get(activity, VideoDetailViewModel::class.java)

    fun liveChannel(activity: FragmentActivity): LiveChannelViewModel =
        get(activity, LiveChannelViewModel::class.java)

    fun favorite(activity: FragmentActivity): FavoriteViewModel =
        get(activity, FavoriteViewModel::class.java)

    fun history(activity: FragmentActivity): HistoryViewModel =
        get(activity, HistoryViewModel::class.java)

    fun settings(activity: FragmentActivity): SettingsViewModel =
        get(activity, SettingsViewModel::class.java)

    fun backup(activity: FragmentActivity): BackupViewModel =
        get(activity, BackupViewModel::class.java)
}
