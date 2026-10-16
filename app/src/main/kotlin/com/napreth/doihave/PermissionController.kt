package com.napreth.doihave

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.widget.Toast

/** Authorization side effects are reachable only through a row click. */
internal class PermissionController(private val activity: Activity, private val preferences: SharedPreferences) {
    fun onClick(entry: PermissionRepository.Entry) {
        val action = PermissionPolicy.onClick(entry.name, Build.VERSION.SDK_INT,
            entry.kind == PermissionRepository.Kind.SPECIAL, entry.granted,
            preferences.getBoolean("requested:${entry.name}", false),
            activity.shouldShowRequestPermissionRationale(entry.name))
        when (action) {
            PermissionPolicy.Action.SPECIAL_SETTINGS -> openSettings(PermissionRepository.specialSettings(activity, entry.name))
            PermissionPolicy.Action.APP_SETTINGS -> openSettings(PermissionRepository.appSettings(activity))
            PermissionPolicy.Action.EXPLAIN_LOCATION -> explain(entry.label, R.string.background_location_explanation)
            PermissionPolicy.Action.EXPLAIN_SENSORS -> explain(entry.label, R.string.background_sensors_explanation)
            PermissionPolicy.Action.REQUEST -> request(entry)
        }
    }

    private fun request(entry: PermissionRepository.Entry) {
        val arguments = PermissionPolicy.requestArguments(entry.name, Build.VERSION.SDK_INT)
        preferences.edit().apply { arguments.forEach { putBoolean("requested:$it", true) } }.apply()
        try { activity.requestPermissions(arguments, REQUEST_CODE) }
        catch (_: IllegalArgumentException) { explain(entry.label, R.string.request_unavailable) }
        catch (_: SecurityException) { explain(entry.label, R.string.request_unavailable) }
    }

    private fun explain(title: String, message: Int) {
        AlertDialog.Builder(activity).setTitle(title).setMessage(message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.open_settings) { _, _ -> openSettings(PermissionRepository.appSettings(activity)) }.show()
    }

    // Package visibility is irrelevant to starting the activity. Attempt it and handle failure.
    internal fun openSettings(intent: Intent) {
        if (!tryStart(intent) && !tryStart(PermissionRepository.appSettings(activity))) {
            Toast.makeText(activity, R.string.settings_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun tryStart(intent: Intent): Boolean = try { activity.startActivity(intent); true }
    catch (_: ActivityNotFoundException) { false }
    catch (_: SecurityException) { false }

    companion object { const val REQUEST_CODE = 100 }
}
