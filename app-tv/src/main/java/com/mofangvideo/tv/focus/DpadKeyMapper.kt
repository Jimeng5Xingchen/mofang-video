package com.mofangvideo.tv.focus

/**
 * TV 端遥控 D-pad 事件枚举。
 *
 * 描述一次方向 / 确定 / 返回 / 菜单按键在焦点引擎中应触发的语义动作，
 * 与 Android KeyEvent 解耦，便于纯 JVM 单测。
 */
enum class TvFocusEvent {
    UP, DOWN, LEFT, RIGHT, CENTER, BACK, MENU, NONE
}

/**
 * 把遥控 / 按键的 keyCode（整数）映射为 [TvFocusEvent]。
 *
 * 纯函数，不依赖 android.view.KeyEvent，JVM 可直接测试。
 * keyCode 取值与 [android.view.KeyEvent] 常量一致：
 * - 19 UP / 20 DOWN / 21 LEFT / 22 RIGHT / 23 CENTER
 * - 4 BACK / 82 MENU
 */
object DpadKeyMapper {

    const val KEY_UP = 19
    const val KEY_DOWN = 20
    const val KEY_LEFT = 21
    const val KEY_RIGHT = 22
    const val KEY_CENTER = 23
    const val KEY_BACK = 4
    const val KEY_MENU = 82

    fun map(keyCode: Int): TvFocusEvent {
        return when (keyCode) {
            KEY_UP -> TvFocusEvent.UP
            KEY_DOWN -> TvFocusEvent.DOWN
            KEY_LEFT -> TvFocusEvent.LEFT
            KEY_RIGHT -> TvFocusEvent.RIGHT
            KEY_CENTER -> TvFocusEvent.CENTER
            KEY_BACK -> TvFocusEvent.BACK
            KEY_MENU -> TvFocusEvent.MENU
            else -> TvFocusEvent.NONE
        }
    }
}
