package com.mofangvideo

import android.app.Application
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp

/**
 * 魔方影视 Application。
 * 启用 Material 3 动态配色（Android 12+ 取壁纸色，低版本回退默认色板）。
 */
@HiltAndroidApp
class MofangApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
