package com.napreth.doihave

import com.napreth.doihave.PermissionPolicy.Action
import com.napreth.doihave.PermissionPolicy.Action.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class PermissionPolicyTest(
    description: String, private val permission: String, private val sdk: Int,
    private val special: Boolean, private val granted: Boolean,
    private val requested: Boolean, private val rationale: Boolean, private val expected: Action,
) {
    @Test fun routesOnlyTheSelectedPermission() {
        assertEquals(expected, PermissionPolicy.onClick(permission, sdk, special, granted, requested, rationale))
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = listOf(
            row("first tap requests", "CAMERA", 35, false, false, false, false, REQUEST),
            row("denied with rationale retries", "CAMERA", 35, false, false, true, true, REQUEST),
            row("permanent denial opens settings", "CAMERA", 35, false, false, true, false, APP_SETTINGS),
            row("granted opens settings", "CAMERA", 35, false, true, false, false, APP_SETTINGS),
            row("special uses dedicated settings", "MANAGE_MEDIA", 35, true, false, false, false, SPECIAL_SETTINGS),
            row("granted special still uses its page", "MANAGE_MEDIA", 35, true, true, true, false, SPECIAL_SETTINGS),
            row("Android 10 background location requests", "ACCESS_BACKGROUND_LOCATION", 29, false, false, false, false, REQUEST),
            row("Android 11 background location explains", "ACCESS_BACKGROUND_LOCATION", 30, false, false, false, false, EXPLAIN_LOCATION),
            row("background location granted", "ACCESS_BACKGROUND_LOCATION", 35, false, true, true, false, APP_SETTINGS),
            row("old background sensors requests", "BODY_SENSORS_BACKGROUND", 32, false, false, false, false, REQUEST),
            row("Android 13 background sensors explains", "BODY_SENSORS_BACKGROUND", 33, false, false, false, false, EXPLAIN_SENSORS),
            row("ordinary sensor still requests", "BODY_SENSORS", 35, false, false, false, false, REQUEST),
        )

        private fun row(description: String, name: String, sdk: Int, special: Boolean,
            granted: Boolean, requested: Boolean, rationale: Boolean, expected: Action) =
            arrayOf<Any>(description, "android.permission.$name", sdk, special, granted, requested, rationale, expected)
    }
}
