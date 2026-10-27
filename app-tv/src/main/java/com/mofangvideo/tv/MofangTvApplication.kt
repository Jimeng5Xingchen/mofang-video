package com.mofangvideo.tv

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp

/**
 * TV 端 Application：启动时按用户选择的主题模式应用深色模式
 * （与手机端 [com.mofangvideo.MofangApplication] 同源读取 SharedPreferences）。
 */
@HiltAndroidApp
class MofangTvApplication : Application() {

    override fun onCreate() {
        super.onCreate()
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
