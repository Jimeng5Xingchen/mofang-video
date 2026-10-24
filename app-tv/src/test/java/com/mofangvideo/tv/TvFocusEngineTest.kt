package com.mofangvideo.tv

import com.mofangvideo.tv.focus.TvFocusEngine
import com.mofangvideo.tv.focus.TvSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 顶栏分节焦点状态机纯逻辑单测（JVM）。
 */
class TvFocusEngineTest {

    @Test
    fun `moves right across sections until last`() {
        val engine = TvFocusEngine()
        assertEquals(TvSection.VIDEO, engine.section)
        assertTrue(engine.moveRight())
        assertEquals(TvSection.LIVE, engine.section)
        assertTrue(engine.moveRight())
        assertEquals(TvSection.FAVORITE, engine.section)
        // walk to the end
        while (engine.moveRight()) {
            // no-op
        }
        assertEquals(TvSection.BACKUP, engine.section)
        // last item: cannot move further right
        assertFalse(engine.moveRight())
        assertEquals(TvSection.BACKUP, engine.section)
    }

    @Test
    fun `moves left back to first`() {
        val engine = TvFocusEngine()
        engine.setSection(TvSection.BACKUP)
        while (engine.moveLeft()) {
            // no-op
        }
        assertEquals(TvSection.VIDEO, engine.section)
        assertFalse(engine.moveLeft())
        assertEquals(TvSection.VIDEO, engine.section)
    }

    @Test
    fun `setSection forces focus`() {
        val engine = TvFocusEngine()
        engine.setSection(TvSection.HISTORY)
        assertEquals(TvSection.HISTORY, engine.section)
        assertEquals(3, engine.sectionIndex())
    }

    @Test
    fun `byIndex resolves within range`() {
        assertEquals(TvSection.VIDEO, TvSection.byIndex(0))
        assertEquals(TvSection.BACKUP, TvSection.byIndex(5))
        assertEquals(null, TvSection.byIndex(6))
        assertEquals(null, TvSection.byIndex(-1))
    }

    @Test
    fun `enter and back preserve section`() {
        val engine = TvFocusEngine()
        engine.setSection(TvSection.LIVE)
        assertEquals(1, engine.enterContent())
        assertEquals(1, engine.backToHeader())
        assertEquals(TvSection.LIVE, engine.selectCurrent())
    }
}
