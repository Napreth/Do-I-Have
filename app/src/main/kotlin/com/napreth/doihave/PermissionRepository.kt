package com.napreth.doihave

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

internal object PermissionRepository {
    enum class Kind { RUNTIME, SPECIAL }
    data class Entry(val name: String, val label: String, val kind: Kind, val state: PermissionState) {
        val granted get() = state.granted
    }

    private val priority = listOf(
        "CAMERA", "RECORD_AUDIO", "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION",
        "ACCESS_BACKGROUND_LOCATION", "POST_NOTIFICATIONS", "READ_CONTACTS", "WRITE_CONTACTS",
        "READ_CALENDAR", "WRITE_CALENDAR", "READ_MEDIA_IMAGES", "READ_MEDIA_VIDEO",
        "READ_MEDIA_AUDIO", "READ_MEDIA_VISUAL_USER_SELECTED", "BLUETOOTH_SCAN",
        "BLUETOOTH_CONNECT", "NEARBY_WIFI_DEVICES",
    )

    @Suppress("DEPRECATION")
    fun load(context: Context): List<Entry> {
        val pm = context.packageManager
        val declared = pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty()
        return declared.mapNotNull { permission ->
            val definition = try { pm.getPermissionInfo(permission, 0) }
            catch (_: PackageManager.NameNotFoundException) { return@mapNotNull null }
            val kind = when {
                // Launchability queries are visibility-filtered. Try starting only after a tap.
                PermissionPolicy.supportsSpecialSettings(permission, Build.VERSION.SDK_INT) -> Kind.SPECIAL
                PermissionPolicy.isRuntimeRequestable(permission, Build.VERSION.SDK_INT, definition.protectionLevel) -> Kind.RUNTIME
                else -> return@mapNotNull null
            }
            Entry(permission, localizedLabel(context, definition), kind, state(context, permission, kind))
        }.sortedWith(compareBy<Entry> {
            priority.indexOf(PermissionPolicy.shortName(it.name)).takeIf { index -> index >= 0 }
                ?: (100 + it.kind.ordinal)
        }.thenBy { it.name })
    }

    private fun localizedLabel(context: Context, definition: PermissionInfo): String {
        val fallback = PermissionLabels.resourceFor(definition.name)
        if (PermissionPolicy.hasSpecialSettings(definition.name) && fallback != 0) return context.getString(fallback)
        var candidate = definition.nonLocalizedLabel?.toString()
        if (definition.labelRes != 0) {
            try {
                candidate = context.createPackageContext(definition.packageName, 0)
                    .createConfigurationContext(context.resources.configuration).getText(definition.labelRes).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                // Use the framework or bundled fallback below.
            } catch (_: Resources.NotFoundException) {
                // OEM resources may not contain the advertised label.
            }
        }
        if (!PermissionLabels.isDisplayName(candidate, definition.name)) {
            candidate = definition.loadLabel(context.packageManager)?.toString()
        }
        return if (PermissionLabels.isDisplayName(candidate, definition.name)) candidate!!.trim()
        else if (fallback != 0) context.getString(fallback) else PermissionLabels.humanize(definition.name)
    }

    fun permissionGrant(context: Context, permission: String): GrantState = try {
        (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED).asGrant()
    } catch (_: SecurityException) { GrantState.UNKNOWN }

    fun state(context: Context, permission: String, kind: Kind): PermissionState {
        val grant = permissionGrant(context, permission)
        if (kind == Kind.RUNTIME) return PermissionState(grant)
        if (PermissionPolicy.shortName(permission) == "SCHEDULE_EXACT_ALARM" && Build.VERSION.SDK_INT >= 31) {
            // This public query accepts the AOSP op string; no hidden constants or reflection.
            val mode = readOrNull("App Op for $permission") { OpsRepository.rawMode(context, "android:schedule_exact_alarm") }
            val capability = readOrNull("Exact-alarm capability") { context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() }
            return PermissionState.exactAlarm(grant, mode, capability)
        }
        val allowed = readOrNull("Special access for $permission") { specialAccessAvailable(context, permission) }
        return PermissionState(grant, allowed?.asGrant() ?: GrantState.UNKNOWN, allowed)
    }

    @Suppress("DEPRECATION")
    private fun specialAccessAvailable(context: Context, permission: String): Boolean =
        when (PermissionPolicy.shortName(permission)) {
            "SYSTEM_ALERT_WINDOW" -> Settings.canDrawOverlays(context)
            "WRITE_SETTINGS" -> Settings.System.canWrite(context)
            "PACKAGE_USAGE_STATS" -> OpsRepository.rawMode(context, "android:get_usage_stats") == AppOpsManager.MODE_ALLOWED
            "MANAGE_EXTERNAL_STORAGE" -> Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()
            "REQUEST_INSTALL_PACKAGES" -> context.packageManager.canRequestPackageInstalls()
            "ACCESS_NOTIFICATION_POLICY" -> context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
            "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" -> context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
            "MANAGE_MEDIA" -> Build.VERSION.SDK_INT >= 31 && MediaStore.canManageMedia(context)
            "USE_FULL_SCREEN_INTENT" -> Build.VERSION.SDK_INT >= 34 && context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
            else -> false
        }

    private fun <T> readOrNull(description: String, read: () -> T): T? = readPermissionState(
        onUnavailable = { error -> Log.w("DoIHave", "Cannot read $description", error) },
        read = read,
    )

    fun appSettings(context: Context) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))
    private fun packageUri(context: Context) = Uri.parse("package:${context.packageName}")

    // Manual diagnostic action, invoked exclusively after the user taps the corresponding row.
    @SuppressLint("BatteryLife")
    fun specialSettings(context: Context, permission: String): Intent {
        val action = when (PermissionPolicy.shortName(permission)) {
            "SYSTEM_ALERT_WINDOW" -> Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            "WRITE_SETTINGS" -> Settings.ACTION_MANAGE_WRITE_SETTINGS
            "PACKAGE_USAGE_STATS" -> Settings.ACTION_USAGE_ACCESS_SETTINGS
            "MANAGE_EXTERNAL_STORAGE" -> if (Build.VERSION.SDK_INT >= 30) Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION else null
            "SCHEDULE_EXACT_ALARM" -> if (Build.VERSION.SDK_INT >= 31) Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM else null
            "REQUEST_INSTALL_PACKAGES" -> Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
            "ACCESS_NOTIFICATION_POLICY" -> Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
            "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" -> Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            "MANAGE_MEDIA" -> if (Build.VERSION.SDK_INT >= 31) Settings.ACTION_REQUEST_MANAGE_MEDIA else null
            "USE_FULL_SCREEN_INTENT" -> if (Build.VERSION.SDK_INT >= 34) Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT else null
            else -> null
        } ?: return appSettings(context)
        return Intent(action).apply {
            if (action != Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS) data = packageUri(context)
        }
    }
}
