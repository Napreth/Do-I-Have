package com.napreth.doihave

import org.junit.Assert.*
import org.junit.Test

internal class PermissionStateTest {
    @Test fun availableCapabilityDoesNotGrantTheExactAlarmSwitch() {
        for (mode in listOf(1, 2)) {
            val state = PermissionState.exactAlarm(permissionGrant = GrantState.GRANTED, mode = mode, canSchedule = true)
            assertEquals(GrantState.GRANTED, state.permissionGrant)
            assertEquals(GrantState.DENIED, state.specialAccess)
            assertEquals(true, state.capabilityAvailable)
            assertFalse(state.granted)
        }
    }
    @Test fun exactAlarmSwitchCanBeGrantedWithoutAStandardPermissionGrant() {
        val state = PermissionState.exactAlarm(GrantState.DENIED, 0, true)
        assertEquals(GrantState.DENIED, state.permissionGrant)
        assertEquals(GrantState.GRANTED, state.specialAccess)
        assertTrue(state.granted)
    }
    @Test fun capabilityFailureDoesNotRevokeAnObservedGrant() {
        val state = PermissionState.exactAlarm(GrantState.DENIED, 0, false)
        assertTrue(state.granted)
        assertEquals(false, state.capabilityAvailable)
    }
    @Test fun defaultAndUnreadableModesDoNotInventGrantOrDenial() {
        for (mode in listOf(null, -1, 3, 4, 999)) {
            val state = PermissionState.exactAlarm(GrantState.GRANTED, mode, true)
            assertEquals(GrantState.UNKNOWN, state.authorization)
            assertFalse(state.granted)
            assertEquals(mode, state.rawAppOpMode)
        }
    }
    @Test fun runtimePermissionsDoNotHaveInferredCapabilityStates() {
        assertTrue(PermissionState.permission(true).granted)
        assertFalse(PermissionState.permission(false).granted)
        assertNull(PermissionState.permission(true).capabilityAvailable)
        assertNull(PermissionState.permission(true).specialAccess)
    }
    @Test fun eachAuthorizationStateBelongsToOneFilter() {
        for (grant in GrantState.entries) {
            val state = PermissionState(GrantState.DENIED, grant, true)
            assertTrue(PermissionFilter.ALL.matches(state))
            assertEquals(1, PermissionFilter.entries.count { it != PermissionFilter.ALL && it.matches(state) })
        }
        assertEquals(PermissionFilter.ALL, PermissionFilter.at(999))
    }
    @Test fun unknownSpecialAccessIsNotMisclassifiedAsDenied() {
        val state = PermissionState.exactAlarm(GrantState.DENIED, 3, true)
        assertFalse(PermissionFilter.GRANTED.matches(state))
        assertFalse(PermissionFilter.DENIED.matches(state))
        assertTrue(PermissionFilter.UNKNOWN.matches(state))
    }
    @Test fun unreadableManifestPermissionIsAlsoKeptUnknown() {
        val state = PermissionState.exactAlarm(GrantState.UNKNOWN, 2, null)
        assertEquals(GrantState.UNKNOWN, state.permissionGrant)
        assertEquals(GrantState.DENIED, state.authorization)
        assertNull(state.capabilityAvailable)
    }
}
