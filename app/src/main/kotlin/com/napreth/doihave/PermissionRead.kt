package com.napreth.doihave

/** Expected platform denials are unknown; programming errors must reach the caller. */
internal fun <T> readPermissionState(onUnavailable: (RuntimeException) -> Unit, read: () -> T): T? =
    try {
        read()
    } catch (error: SecurityException) {
        onUnavailable(error)
        null
    } catch (error: IllegalArgumentException) {
        onUnavailable(error)
        null
    } catch (error: UnsupportedOperationException) {
        onUnavailable(error)
        null
    }
