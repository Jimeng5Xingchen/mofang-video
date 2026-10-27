package com.mofangvideo.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun `patch 位进位比较`() {
        assertTrue(VersionComparator.isNewer("v1.2.10", "1.2.9"))
        assertFalse(VersionComparator.isNewer("1.2.9", "v1.2.10"))
    }

    @Test
    fun `同版本返回非更新`() {
        assertFalse(VersionComparator.isNewer("v1.1.0", "1.1.0"))
        assertFalse(VersionComparator.isNewer("v1.0.0", "1.1.0"))
    }

    @Test
    fun `major 与 minor 逐段比较`() {
        assertTrue(VersionComparator.isNewer("2.0.0", "1.9.9"))
        assertTrue(VersionComparator.isNewer("1.10.0", "1.9.9"))
    }

    @Test
    fun `预发布后缀视为旧于正式版`() {
        assertTrue(VersionComparator.isNewer("v1.2.0", "1.2.0-beta.1"))
        assertFalse(VersionComparator.isNewer("v1.2.0-beta.1", "1.2.0"))
    }

    @Test
    fun `同后缀字符串比较`() {
        assertTrue(VersionComparator.isNewer("1.2.0-beta.2", "1.2.0-beta.1"))
        assertFalse(VersionComparator.isNewer("1.2.0-beta.1", "1.2.0-beta.1"))
    }

    @Test
    fun `非法版本解析为 null 且不判更新`() {
        assertNull(VersionComparator.parse("abc"))
        assertNull(VersionComparator.parse("1.2"))
        assertFalse(VersionComparator.isNewer("abc", "1.0.0"))
        assertFalse(VersionComparator.isNewer("1.1.0", "abc"))
    }
}
