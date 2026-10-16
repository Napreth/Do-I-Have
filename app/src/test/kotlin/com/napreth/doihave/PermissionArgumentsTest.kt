package com.napreth.doihave

import org.junit.Assert.*
import org.junit.Test

internal class PermissionArgumentsTest {
    private val prefix = "android.permission."
    private fun args(name: String, sdk: Int) = PermissionPolicy.requestArguments(prefix + name, sdk)

    @Test fun fineLocationGroupsApproximateLocationStartingAtAndroid12() {
        assertArrayEquals(arrayOf(prefix + "ACCESS_FINE_LOCATION"), args("ACCESS_FINE_LOCATION", 30))
        assertArrayEquals(arrayOf(prefix + "ACCESS_FINE_LOCATION", prefix + "ACCESS_COARSE_LOCATION"), args("ACCESS_FINE_LOCATION", 31))
    }
    @Test fun coarseLocationNeverRequestsFineLocation() {
        assertArrayEquals(arrayOf(prefix + "ACCESS_COARSE_LOCATION"), args("ACCESS_COARSE_LOCATION", 35))
    }
    @Test fun imagesIncludeSelectedMediaOnlyFromAndroid14() {
        assertArrayEquals(arrayOf(prefix + "READ_MEDIA_IMAGES"), args("READ_MEDIA_IMAGES", 33))
        assertArrayEquals(arrayOf(prefix + "READ_MEDIA_IMAGES", prefix + "READ_MEDIA_VISUAL_USER_SELECTED"), args("READ_MEDIA_IMAGES", 34))
    }
    @Test fun videoIncludesSelectedMedia() {
        assertArrayEquals(arrayOf(prefix + "READ_MEDIA_VIDEO", prefix + "READ_MEDIA_VISUAL_USER_SELECTED"), args("READ_MEDIA_VIDEO", 34))
    }
    @Test fun selectedMediaIncludesBothVisualTypes() {
        assertArrayEquals(arrayOf(prefix + "READ_MEDIA_IMAGES", prefix + "READ_MEDIA_VIDEO", prefix + "READ_MEDIA_VISUAL_USER_SELECTED"), args("READ_MEDIA_VISUAL_USER_SELECTED", 34))
    }
    @Test fun audioAndCameraDoNotRequestUnrelatedAccess() {
        listOf("READ_MEDIA_AUDIO", "CAMERA", "RECORD_AUDIO").forEach {
            assertArrayEquals(arrayOf(prefix + it), args(it, 35))
        }
    }
    @Test fun normalSignatureAndPrivilegedPermissionsAreNotRuntimeEntries() {
        assertFalse(PermissionPolicy.isRuntimeRequestable(prefix + "INTERNET", 35, 0))
        assertFalse(PermissionPolicy.isRuntimeRequestable(prefix + "WRITE_SECURE_SETTINGS", 35, 0x12))
        assertTrue(PermissionPolicy.isRuntimeRequestable(prefix + "CAMERA", 35, 1))
        assertTrue(PermissionPolicy.isRuntimeRequestable(prefix + "READ_CALL_LOG", 35, 0x801))
    }
    @Test fun obsoleteStoragePermissionsDisappearAtTheirSdkBoundary() {
        assertTrue(PermissionPolicy.isRuntimeRequestable(prefix + "WRITE_EXTERNAL_STORAGE", 29, 1))
        assertFalse(PermissionPolicy.isRuntimeRequestable(prefix + "WRITE_EXTERNAL_STORAGE", 30, 1))
        assertTrue(PermissionPolicy.isRuntimeRequestable(prefix + "READ_EXTERNAL_STORAGE", 32, 1))
        assertFalse(PermissionPolicy.isRuntimeRequestable(prefix + "READ_EXTERNAL_STORAGE", 33, 1))
    }
    @Test fun specialAccessIsLimitedToImplementedSdkSupportedActions() {
        mapOf("MANAGE_EXTERNAL_STORAGE" to 30, "SCHEDULE_EXACT_ALARM" to 31, "MANAGE_MEDIA" to 31, "USE_FULL_SCREEN_INTENT" to 34).forEach { (name, minimum) ->
            assertFalse(name, PermissionPolicy.supportsSpecialSettings(prefix + name, minimum - 1))
            assertTrue(name, PermissionPolicy.supportsSpecialSettings(prefix + name, minimum))
        }
        assertTrue(PermissionPolicy.supportsSpecialSettings(prefix + "SYSTEM_ALERT_WINDOW", 26))
        assertFalse(PermissionPolicy.supportsSpecialSettings(prefix + "INTERNET", 37))
        assertFalse(PermissionPolicy.supportsSpecialSettings(prefix + "UNKNOWN_FUTURE_PERMISSION", 37))
    }
    @Test fun vendorPermissionNamespacesArePreserved() {
        val voicemail = "com.android.voicemail.permission.ADD_VOICEMAIL"
        assertEquals(voicemail, PermissionPolicy.shortName(voicemail))
        assertArrayEquals(arrayOf(voicemail), PermissionPolicy.requestArguments(voicemail, 35))
    }
}
