package com.napreth.doihave

/** Pure rules; evaluating them never requests access or launches an activity. */
internal object PermissionPolicy {
    private const val PREFIX = "android.permission."
    private val specials = setOf(
        "SYSTEM_ALERT_WINDOW", "WRITE_SETTINGS", "PACKAGE_USAGE_STATS",
        "MANAGE_EXTERNAL_STORAGE", "SCHEDULE_EXACT_ALARM", "REQUEST_INSTALL_PACKAGES",
        "ACCESS_NOTIFICATION_POLICY", "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
        "MANAGE_MEDIA", "USE_FULL_SCREEN_INTENT",
    )

    enum class Action { SPECIAL_SETTINGS, APP_SETTINGS, EXPLAIN_LOCATION, EXPLAIN_SENSORS, REQUEST }

    fun shortName(permission: String) = permission.removePrefix(PREFIX)
    fun hasSpecialSettings(permission: String) = shortName(permission) in specials

    fun supportsSpecialSettings(permission: String, sdk: Int): Boolean =
        hasSpecialSettings(permission) && sdk >= when (shortName(permission)) {
            "MANAGE_EXTERNAL_STORAGE" -> 30
            "SCHEDULE_EXACT_ALARM", "MANAGE_MEDIA" -> 31
            "USE_FULL_SCREEN_INTENT" -> 34
            else -> 26
        }

    fun isRuntimeRequestable(permission: String, sdk: Int, protectionLevel: Int): Boolean =
        protectionLevel and 0xf == 1 && when (shortName(permission)) {
            "WRITE_EXTERNAL_STORAGE" -> sdk < 30
            "READ_EXTERNAL_STORAGE" -> sdk < 33
            else -> true
        }

    fun onClick(
        permission: String, sdk: Int, special: Boolean, granted: Boolean,
        previouslyRequested: Boolean, showRationale: Boolean,
    ): Action = when {
        special -> Action.SPECIAL_SETTINGS
        granted -> Action.APP_SETTINGS
        shortName(permission) == "ACCESS_BACKGROUND_LOCATION" && sdk >= 30 -> Action.EXPLAIN_LOCATION
        shortName(permission) == "BODY_SENSORS_BACKGROUND" && sdk >= 33 -> Action.EXPLAIN_SENSORS
        previouslyRequested && !showRationale -> Action.APP_SETTINGS
        else -> Action.REQUEST
    }

    fun requestArguments(permission: String, sdk: Int): Array<String> = when {
        shortName(permission) == "ACCESS_FINE_LOCATION" && sdk >= 31 ->
            arrayOf(PREFIX + "ACCESS_FINE_LOCATION", PREFIX + "ACCESS_COARSE_LOCATION")
        sdk >= 34 && shortName(permission) in setOf("READ_MEDIA_IMAGES", "READ_MEDIA_VIDEO") ->
            arrayOf(permission, PREFIX + "READ_MEDIA_VISUAL_USER_SELECTED")
        sdk >= 34 && shortName(permission) == "READ_MEDIA_VISUAL_USER_SELECTED" ->
            arrayOf(PREFIX + "READ_MEDIA_IMAGES", PREFIX + "READ_MEDIA_VIDEO", permission)
        else -> arrayOf(permission)
    }
}
