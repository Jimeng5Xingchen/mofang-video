package com.mofangvideo.tv

import com.mofangvideo.tv.focus.DpadKeyMapper
import com.mofangvideo.tv.focus.TvFocusEvent
import com.mofangvideo.tv.focus.TvPlayAction
import com.mofangvideo.tv.focus.TvPlayKeyMapper
import com.mofangvideo.tv.focus.TvSection
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 遥控 D-pad 键位映射纯逻辑单测（JVM，零 Android 依赖）。
 */
class DpadKeyMapperTest {

    @Test
    fun `dpad keycodes map to focus events`() {
        assertEquals(TvFocusEvent.UP, DpadKeyMapper.map(DpadKeyMapper.KEY_UP))
        assertEquals(TvFocusEvent.DOWN, DpadKeyMapper.map(DpadKeyMapper.KEY_DOWN))
        assertEquals(TvFocusEvent.LEFT, DpadKeyMapper.map(DpadKeyMapper.KEY_LEFT))
        assertEquals(TvFocusEvent.RIGHT, DpadKeyMapper.map(DpadKeyMapper.KEY_RIGHT))
        assertEquals(TvFocusEvent.CENTER, DpadKeyMapper.map(DpadKeyMapper.KEY_CENTER))
        assertEquals(TvFocusEvent.BACK, DpadKeyMapper.map(DpadKeyMapper.KEY_BACK))
        assertEquals(TvFocusEvent.MENU, DpadKeyMapper.map(DpadKeyMapper.KEY_MENU))
    }

    @Test
    fun `unknown keycodes map to NONE`() {
        assertEquals(TvFocusEvent.NONE, DpadKeyMapper.map(0))
        assertEquals(TvFocusEvent.NONE, DpadKeyMapper.map(65))
    }

    @Test
    fun `play keycodes map to play actions`() {
        assertEquals(TvPlayAction.SEEK_BACK, TvPlayKeyMapper.map(TvPlayKeyMapper.KEY_LEFT))
        assertEquals(TvPlayAction.SEEK_FWD, TvPlayKeyMapper.map(TvPlayKeyMapper.KEY_RIGHT))
        assertEquals(TvPlayAction.PREVIOUS, TvPlayKeyMapper.map(TvPlayKeyMapper.KEY_UP))
        assertEquals(TvPlayAction.NEXT, TvPlayKeyMapper.map(TvPlayKeyMapper.KEY_DOWN))
        assertEquals(TvPlayAction.PLAY_PAUSE, TvPlayKeyMapper.map(TvPlayKeyMapper.KEY_CENTER))
        assertEquals(TvPlayAction.FULLSCREEN, TvPlayKeyMapper.map(TvPlayKeyMapper.KEY_MENU))
        assertEquals(TvPlayAction.NONE, TvPlayKeyMapper.map(0))
    }
}
