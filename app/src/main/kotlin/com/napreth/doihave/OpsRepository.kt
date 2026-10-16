package com.napreth.doihave

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.util.Locale

internal object OpsRepository {
    data class Entry(
        val id: Int, val name: String, val permission: String,
        val mode: Int, val permissionGrant: GrantState?,
    ) {
        val simpleName get() = name.removePrefix("android:").uppercase(Locale.ROOT)
    }

    fun load(context: Context): List<Entry> = context.assets.open("appops.tsv").reader(Charsets.UTF_8).use { reader ->
        OpsCatalog.read(reader).mapNotNull { op ->
            val mode = try { rawMode(context, op.name) }
            catch (_: IllegalArgumentException) { return@mapNotNull null }
            catch (_: SecurityException) { -1 }
            Entry(op.id, op.name, op.permission, mode,
                op.permission.takeIf { it.isNotEmpty() }?.let { PermissionRepository.permissionGrant(context, it) })
        }
    }

    @Suppress("DEPRECATION")
    fun rawMode(context: Context, operation: String): Int {
        val manager = context.getSystemService(AppOpsManager::class.java)
        return when {
            Build.VERSION.SDK_INT >= 36 -> manager.checkOpRawNoThrow(operation, Process.myUid(), context.packageName, null)
            Build.VERSION.SDK_INT >= 29 -> manager.unsafeCheckOpRawNoThrow(operation, Process.myUid(), context.packageName)
            else -> manager.checkOpNoThrow(operation, Process.myUid(), context.packageName)
        }
    }

    fun modeLabel(context: Context, mode: Int): String = context.getString(when (mode) {
        0 -> R.string.mode_allowed
        1 -> R.string.mode_ignored
        2 -> R.string.mode_errored
        3 -> R.string.mode_default
        4 -> R.string.mode_foreground
        else -> R.string.mode_unknown
    }, mode)
}
