package com.napreth.doihave

import java.util.Locale

/** Localized fallbacks for identifier-only labels in framework/OEM resources. */
internal object PermissionLabels {
    private val identifier = Regex("[A-Z][A-Z0-9_]*")

    fun resourceFor(name: String): Int = when (PermissionPolicy.shortName(name)) {
        "SYSTEM_ALERT_WINDOW" -> R.string.permission_overlay
        "WRITE_SETTINGS" -> R.string.permission_write_settings
        "PACKAGE_USAGE_STATS" -> R.string.permission_usage
        "MANAGE_EXTERNAL_STORAGE" -> R.string.permission_all_files
        "SCHEDULE_EXACT_ALARM" -> R.string.permission_alarms
        "REQUEST_INSTALL_PACKAGES" -> R.string.permission_install
        "ACCESS_NOTIFICATION_POLICY" -> R.string.permission_dnd
        "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" -> R.string.permission_battery
        "MANAGE_MEDIA" -> R.string.permission_manage_media
        "USE_FULL_SCREEN_INTENT" -> R.string.permission_full_screen
        "ACCEPT_HANDOVER" -> R.string.permission_handover
        "CAMERA" -> R.string.permission_camera
        "RECORD_AUDIO" -> R.string.permission_microphone
        "ACCESS_COARSE_LOCATION" -> R.string.permission_coarse_location
        "ACCESS_FINE_LOCATION" -> R.string.permission_fine_location
        "ACCESS_BACKGROUND_LOCATION" -> R.string.permission_background_location
        "POST_NOTIFICATIONS" -> R.string.permission_notifications
        "READ_CONTACTS" -> R.string.permission_read_contacts
        "WRITE_CONTACTS" -> R.string.permission_write_contacts
        "GET_ACCOUNTS" -> R.string.permission_accounts
        "READ_CALENDAR" -> R.string.permission_read_calendar
        "WRITE_CALENDAR" -> R.string.permission_write_calendar
        "READ_CALL_LOG" -> R.string.permission_read_calls
        "WRITE_CALL_LOG" -> R.string.permission_write_calls
        "READ_PHONE_STATE" -> R.string.permission_phone_state
        "READ_PHONE_NUMBERS" -> R.string.permission_phone_numbers
        "CALL_PHONE" -> R.string.permission_call
        "ANSWER_PHONE_CALLS" -> R.string.permission_answer
        "PROCESS_OUTGOING_CALLS" -> R.string.permission_outgoing_calls
        "USE_SIP" -> R.string.permission_sip
        "com.android.voicemail.permission.ADD_VOICEMAIL" -> R.string.permission_voicemail
        "READ_SMS" -> R.string.permission_read_sms
        "SEND_SMS" -> R.string.permission_send_sms
        "RECEIVE_SMS" -> R.string.permission_receive_sms
        "RECEIVE_MMS" -> R.string.permission_receive_mms
        "RECEIVE_WAP_PUSH" -> R.string.permission_wap
        "READ_CELL_BROADCASTS" -> R.string.permission_cell_broadcasts
        "BODY_SENSORS" -> R.string.permission_sensors
        "BODY_SENSORS_BACKGROUND" -> R.string.permission_background_sensors
        "ACTIVITY_RECOGNITION" -> R.string.permission_activity
        "READ_MEDIA_IMAGES" -> R.string.permission_images
        "READ_MEDIA_VIDEO" -> R.string.permission_video
        "READ_MEDIA_AUDIO" -> R.string.permission_audio
        "READ_MEDIA_VISUAL_USER_SELECTED" -> R.string.permission_selected_media
        "ACCESS_MEDIA_LOCATION" -> R.string.permission_media_location
        "READ_EXTERNAL_STORAGE" -> R.string.permission_read_storage
        "WRITE_EXTERNAL_STORAGE" -> R.string.permission_write_storage
        "BLUETOOTH_SCAN" -> R.string.permission_bluetooth_scan
        "BLUETOOTH_CONNECT" -> R.string.permission_bluetooth_connect
        "BLUETOOTH_ADVERTISE" -> R.string.permission_bluetooth_advertise
        "NEARBY_WIFI_DEVICES" -> R.string.permission_wifi
        "UWB_RANGING", "RANGING" -> R.string.permission_ranging
        "ACCESS_LOCAL_NETWORK" -> R.string.permission_local_network
        else -> 0
    }

    fun isDisplayName(label: String?, permission: String): Boolean {
        val value = label?.trim().orEmpty()
        return value.isNotEmpty() && value != permission && !value.contains(".permission.") && !identifier.matches(value)
    }

    fun humanize(permission: String): String = permission.substringAfterLast('.')
        .replace('_', ' ').lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
}
