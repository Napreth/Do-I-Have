package com.napreth.doihave

import android.app.Activity
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class PermissionAccessTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun settingsLaunchDoesNotPreflightVisibilityQueries() = onMain {
        val activity = RecordingActivity()
        controller(activity).openSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        assertEquals(listOf(Settings.ACTION_USAGE_ACCESS_SETTINGS), activity.attempts.map { it.action })
    }

    @Test fun missingSettingsActivityFallsBackToApplicationSettings() = onMain {
        val activity = RecordingActivity(ActivityNotFoundException())
        controller(activity).openSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        assertEquals(2, activity.attempts.size)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, activity.attempts.last().action)
        assertEquals("package:${context.packageName}", activity.attempts.last().data.toString())
    }

    @Test fun blockedSettingsActivityAlsoFallsBack() = onMain {
        val activity = RecordingActivity(SecurityException())
        controller(activity).openSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, activity.attempts.last().action)
    }

    @Suppress("DEPRECATION")
    @Test fun manifestDoesNotGrantTheCompetingExactAlarmPermission() {
        val declared = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertTrue("android.permission.SCHEDULE_EXACT_ALARM" in declared)
        assertFalse("android.permission.USE_EXACT_ALARM" in declared)
    }

    @Test fun everySupportedSpecialAccessEntryIsRetained() {
        val names = PermissionRepository.load(context).map { it.name }.toSet()
        for (name in listOf("SYSTEM_ALERT_WINDOW", "WRITE_SETTINGS", "PACKAGE_USAGE_STATS", "MANAGE_EXTERNAL_STORAGE",
            "SCHEDULE_EXACT_ALARM", "REQUEST_INSTALL_PACKAGES", "ACCESS_NOTIFICATION_POLICY",
            "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS", "MANAGE_MEDIA", "USE_FULL_SCREEN_INTENT")) {
            if (PermissionPolicy.supportsSpecialSettings(name, Build.VERSION.SDK_INT)) {
                assertTrue("Special access was filtered out: $name", "android.permission.$name" in names)
            }
        }
    }

    @Test fun exactAlarmGrantComesFromItsOpAndCapabilityIsRecordedSeparately() {
        if (Build.VERSION.SDK_INT < 31) return
        val state = PermissionRepository.state(context, "android.permission.SCHEDULE_EXACT_ALARM", PermissionRepository.Kind.SPECIAL)
        val mode = OpsRepository.rawMode(context, "android:schedule_exact_alarm")
        assertEquals(mode, state.rawAppOpMode)
        assertEquals(context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(), state.capabilityAvailable)
        assertEquals(when (mode) { 0 -> GrantState.GRANTED; 1, 2 -> GrantState.DENIED; else -> GrantState.UNKNOWN }, state.authorization)
    }

    private fun controller(activity: Activity) = PermissionController(activity, context.getSharedPreferences("test-launch", 0))
    private fun onMain(assertions: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(assertions)

    private inner class RecordingActivity(private val firstFailure: RuntimeException? = null) : Activity() {
        val attempts = mutableListOf<Intent>()
        init { attachBaseContext(context) }
        override fun getPackageManager(): PackageManager = throw AssertionError("Launch must not query package visibility")
        override fun startActivity(intent: Intent) {
            attempts += intent
            if (attempts.size == 1 && firstFailure != null) throw firstFailure
        }
    }
}
