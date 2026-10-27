package com.mofangvideo

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp

/**
 * 魔方影视 Application。
 * 启用 Material 3 动态配色（Android 12+ 取壁纸色，低版本回退默认色板）；
 * 启动时按用户选择的主题模式应用深色模式。
 */
@HiltAndroidApp
class MofangApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        AppCompatDelegate.setDefaultNightMode(nightModeOf(storedThemeMode()))
    }

    private fun storedThemeMode(): String =
        getSharedPreferences("mofang_settings", Context.MODE_PRIVATE)
            .getString("theme_mode", "system") ?: "system"

    private fun nightModeOf(mode: String): Int = when (mode) {
        "light" -> AppCompatDelegate.MODE_NIGHT_NO
        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}
