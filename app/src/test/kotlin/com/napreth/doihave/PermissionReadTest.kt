package com.napreth.doihave

import org.junit.Assert.*
import org.junit.Test

internal class PermissionReadTest {
    @Test fun successfulReadsAreReturnedWithoutReportingFailure() {
        val failures = mutableListOf<RuntimeException>()
        assertEquals(42, readPermissionState({ failures += it }) { 42 })
        assertTrue(failures.isEmpty())
    }

    @Test fun nullablePlatformResultsDoNotInventExceptions() {
        val failures = mutableListOf<RuntimeException>()
        assertNull(readPermissionState({ failures += it }) { null })
        assertTrue(failures.isEmpty())
    }

    @Test fun deniedQueriesAreUnknownAndReported() = expectedFailure(SecurityException("denied"))
    @Test fun unsupportedOperationsAreUnknownAndReported() = expectedFailure(IllegalArgumentException("unsupported op"))
    @Test fun missingPlatformFeaturesAreUnknownAndReported() = expectedFailure(UnsupportedOperationException("feature missing"))

    @Test fun nullPointerBugsPropagate() = unexpectedFailure(NullPointerException("bug"))
    @Test fun illegalStateBugsPropagate() = unexpectedFailure(IllegalStateException("bug"))
    @Test fun otherRuntimeBugsPropagate() = unexpectedFailure(RuntimeException("bug"))

    private fun expectedFailure(error: RuntimeException) {
        val failures = mutableListOf<RuntimeException>()
        val result = readPermissionState({ failures += it }) { throw error }
        assertNull(result)
        assertEquals(listOf(error), failures)
    }

    private fun unexpectedFailure(error: RuntimeException) {
        val failures = mutableListOf<RuntimeException>()
        val propagated = assertThrows(error.javaClass) {
            readPermissionState({ failures += it }) { throw error }
        }
        assertSame(error, propagated)
        assertTrue(failures.isEmpty())
    }
}
