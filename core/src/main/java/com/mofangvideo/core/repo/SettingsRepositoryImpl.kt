package com.mofangvideo.core.repo

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 设置仓库实现，基于 SharedPreferences。
 */
class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    private val prefs by lazy {
        context.getSharedPreferences("mofang_settings", Context.MODE_PRIVATE)
    }

    override fun playbackCore(): Flow<String> =
        flowOf(prefs.getString("playback_core", "exo") ?: "exo")

    override fun setPlaybackCore(core: String) {
        prefs.edit().putString("playback_core", core).apply()
    }

    override fun autoResume(): Flow<Boolean> =
        flowOf(prefs.getBoolean("auto_resume", true))

    override fun setAutoResume(enable: Boolean) {
        prefs.edit().putBoolean("auto_resume", enable).apply()
    }

    override fun defaultQuality(): Flow<String> =
        flowOf(prefs.getString("default_quality", "auto") ?: "auto")

    override fun setDefaultQuality(quality: String) {
        prefs.edit().putString("default_quality", quality).apply()
    }
}
