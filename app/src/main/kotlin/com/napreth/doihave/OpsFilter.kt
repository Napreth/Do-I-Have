package com.napreth.doihave

internal enum class OpsFilter(val label: Int, private val mode: Int? = null) {
    ALL(R.string.all), ALLOWED(R.string.filter_allowed, 0),
    FOREGROUND(R.string.filter_foreground, 4), IGNORED(R.string.filter_ignored, 1),
    DENIED(R.string.filter_errored, 2), DEFAULT(R.string.filter_default, 3),
    UNKNOWN(R.string.filter_unknown);

    fun matches(value: Int) = when (this) {
        ALL -> true
        UNKNOWN -> value !in 0..4
        else -> value == mode
    }

    companion object {
        fun at(index: Int) = entries.getOrNull(index) ?: ALL
        fun labels() = entries.map { it.label }.toIntArray()
    }
}
