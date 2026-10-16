package com.napreth.doihave

import java.io.IOException
import java.io.Reader

internal object OpsCatalog {
    data class Operation(val id: Int, val name: String, val permission: String)
    private val operationName = Regex("android:[a-z0-9_]+")

    /** The caller owns and closes the reader. */
    fun read(source: Reader): List<Operation> {
        val ids = mutableSetOf<Int>()
        val names = mutableSetOf<String>()
        val result = mutableListOf<Operation>()
        source.buffered().lineSequence().forEachIndexed { index, line ->
            if (line.startsWith('#') || line.isBlank()) return@forEachIndexed
            val fields = line.split('\t')
            val id = fields.firstOrNull()?.toIntOrNull()
            if (fields.size != 3 || id == null || id < 0 || !operationName.matches(fields[1]) ||
                (fields[2].isNotEmpty() && !fields[2].contains(".permission.")) ||
                !ids.add(id) || !names.add(fields[1])
            ) throw IOException("Invalid or duplicate AppOps catalog entry at line ${index + 1}")
            result += Operation(id, fields[1], fields[2])
        }
        return result.toList()
    }
}
