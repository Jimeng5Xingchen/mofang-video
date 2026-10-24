package com.mofangvideo.tv.focus

/**
 * 播放页专用遥控动作枚举。
 *
 * 播放页无卡片列表焦点，D-pad 语义与浏览页不同：
 * - 左右 → 快退 / 快进（点播）
 * - 上下 → 上一个 / 下一个频道（直播）或 上一集 / 下一集（点播）
 * - 中键 → 播放 / 暂停
 */
enum class TvPlayAction {
    SEEK_BACK, SEEK_FWD, PREVIOUS, NEXT, PLAY_PAUSE, FULLSCREEN, NONE
}

/**
 * 把播放页 keyCode 映射为 [TvPlayAction]。
 *
 * 纯函数，JVM 可直接测试。
 */
object TvPlayKeyMapper {

    const val KEY_UP = 19
    const val KEY_DOWN = 20
    const val KEY_LEFT = 21
    const val KEY_RIGHT = 22
    const val KEY_CENTER = 23
    const val KEY_MENU = 82

    /**
     * @param isLive 直播页时上下键切频道；点播页上下键切集
     */
    fun map(keyCode: Int): TvPlayAction {
        return when (keyCode) {
            KEY_LEFT -> TvPlayAction.SEEK_BACK
            KEY_RIGHT -> TvPlayAction.SEEK_FWD
            KEY_UP -> TvPlayAction.PREVIOUS
            KEY_DOWN -> TvPlayAction.NEXT
            KEY_CENTER -> TvPlayAction.PLAY_PAUSE
            KEY_MENU -> TvPlayAction.FULLSCREEN
            else -> TvPlayAction.NONE
        }
    }
}
