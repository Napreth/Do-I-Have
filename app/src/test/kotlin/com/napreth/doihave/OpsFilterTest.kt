package com.napreth.doihave

import org.junit.Assert.*
import org.junit.Test

internal class OpsFilterTest {
    @Test fun knownModesAndUnknownModesBelongToExactlyOneStateFilter() {
        listOf(Int.MIN_VALUE, -1, 0, 1, 2, 3, 4, 5, Int.MAX_VALUE).forEach { mode ->
            assertEquals("Mode $mode", 1, OpsFilter.entries.count { it != OpsFilter.ALL && it.matches(mode) })
            assertTrue(OpsFilter.ALL.matches(mode))
        }
    }
    @Test fun foregroundDoesNotCountAsUnconditionallyAllowed() {
        assertTrue(OpsFilter.ALLOWED.matches(0))
        assertFalse(OpsFilter.ALLOWED.matches(4))
        assertTrue(OpsFilter.FOREGROUND.matches(4))
        assertFalse(OpsFilter.FOREGROUND.matches(0))
    }
    @Test fun remainingModesKeepTheirAndroidMeanings() {
        assertTrue(OpsFilter.IGNORED.matches(1))
        assertTrue(OpsFilter.DENIED.matches(2))
        assertTrue(OpsFilter.DEFAULT.matches(3))
        assertTrue(OpsFilter.UNKNOWN.matches(-1))
        assertTrue(OpsFilter.UNKNOWN.matches(99))
    }
    @Test fun invalidSavedSelectionFallsBackToAll() {
        assertEquals(OpsFilter.ALL, OpsFilter.at(-1))
        assertEquals(OpsFilter.ALL, OpsFilter.at(999))
        assertEquals(OpsFilter.FOREGROUND, OpsFilter.at(2))
        assertEquals(7, OpsFilter.labels().size)
    }
}
