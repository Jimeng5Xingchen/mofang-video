package com.mofangvideo.tv.focus

import com.mofangvideo.tv.focus.TvSection.Companion.ALL

/**
 * 顶栏分节焦点状态机。
 *
 * 管理顶栏 6 个分节 Tab 的左右移动与"当前选中分节"。
 * 纯逻辑，零 Android 依赖，JVM 可单测。
 */
class TvFocusEngine {

    /** 当前焦点所在分节。 */
    var section: TvSection = TvSection.VIDEO
        private set

    fun sectionIndex(): Int = ALL.indexOf(section)

    /** 左移一个分节，到首项则停留。 */
    fun moveLeft(): Boolean {
        val idx = sectionIndex()
        if (idx > 0) {
            section = ALL[idx - 1]
            return true
        }
        return false
    }

    /** 右移一个分节，到末项则停留。 */
    fun moveRight(): Boolean {
        val idx = sectionIndex()
        if (idx < ALL.size - 1) {
            section = ALL[idx + 1]
            return true
        }
        return false
    }

    /** 下键：进入当前分节内容区（顶栏不变）。 */
    fun enterContent(): Int = sectionIndex()

    /** 上键：从内容区回到顶栏当前分节。 */
    fun backToHeader(): Int = sectionIndex()

    /** 确定键：在当前分节 Tab 上按下，直接选中该分节。 */
    fun selectCurrent(): TvSection = section

    /** 强制设置分节（外部切换分节时调用，焦点回落该分节）。 */
    fun setSection(section: TvSection) {
        this.section = section
    }
}
