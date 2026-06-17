package com.clockin.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {
    @Test
    fun isNewer_detectsPatchBump() {
        assertTrue(VersionComparator.isNewer("1.0.4", "1.0.3"))
        assertFalse(VersionComparator.isNewer("1.0.3", "1.0.3"))
        assertFalse(VersionComparator.isNewer("1.0.2", "1.0.3"))
    }

    @Test
    fun isNewer_handlesTagPrefixAndDoubleDigitPatch() {
        assertTrue(VersionComparator.isNewer("v1.0.10", "1.0.9"))
        assertTrue(VersionComparator.isNewer("1.1.0", "1.0.9"))
    }

    @Test
    fun normalize_stripsVersionPrefix() {
        assertEquals("1.0.4", VersionComparator.normalize("v1.0.4"))
    }
}
