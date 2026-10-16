package com.napreth.doihave

internal enum class PermissionFilter(val label: Int, private val state: GrantState? = null) {
    ALL(R.string.all), GRANTED(R.string.granted, GrantState.GRANTED),
    DENIED(R.string.not_granted, GrantState.DENIED), UNKNOWN(R.string.grant_unknown, GrantState.UNKNOWN);

    fun matches(value: PermissionState) = state == null || value.authorization == state

    companion object {
        fun at(index: Int) = entries.getOrNull(index) ?: ALL
        fun labels() = entries.map { it.label }.toIntArray()
    }
}
