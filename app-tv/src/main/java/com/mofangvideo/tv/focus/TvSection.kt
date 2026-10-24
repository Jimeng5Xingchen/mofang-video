package com.mofangvideo.tv.focus

/**
 * 顶栏分节 Tab 的焦点模型。
 *
 * TV 主壳顶栏为一行可焦点切换的分节（影视 / 直播 / 收藏 / 历史 / 设置 / 备份），
 * 左右键在分节间移动，下键进入当前分节内容区。
 */
enum class TvSection {
    VIDEO, LIVE, FAVORITE, HISTORY, SETTINGS, BACKUP;

    companion object {
        val ALL: List<TvSection> = listOf(VIDEO, LIVE, FAVORITE, HISTORY, SETTINGS, BACKUP)

        fun byIndex(index: Int): TvSection? = ALL.getOrNull(index)
    }
}
