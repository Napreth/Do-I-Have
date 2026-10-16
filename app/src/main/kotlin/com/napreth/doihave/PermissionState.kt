package com.napreth.doihave

/** A permission grant, a special-access switch and an available capability are different facts. */
internal enum class GrantState { GRANTED, DENIED, UNKNOWN }

internal data class PermissionState(
    val permissionGrant: GrantState,
    val specialAccess: GrantState? = null,
    val capabilityAvailable: Boolean? = null,
    val rawAppOpMode: Int? = null,
) {
    val authorization: GrantState get() = specialAccess ?: permissionGrant
    val granted: Boolean get() = authorization == GrantState.GRANTED

    companion object {
        fun permission(granted: Boolean) = PermissionState(granted.asGrant())

        fun exactAlarm(permissionGrant: GrantState, mode: Int?, canSchedule: Boolean?) = PermissionState(
            permissionGrant = permissionGrant,
            // MODE_DEFAULT needs system/OEM policy that public APIs cannot isolate from exemptions.
            specialAccess = when (mode) {
                0 -> GrantState.GRANTED
                1, 2 -> GrantState.DENIED
                else -> GrantState.UNKNOWN
            },
            capabilityAvailable = canSchedule,
            rawAppOpMode = mode,
        )
    }
}

internal fun Boolean.asGrant() = if (this) GrantState.GRANTED else GrantState.DENIED
