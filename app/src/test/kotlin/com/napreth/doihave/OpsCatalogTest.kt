package com.napreth.doihave

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.io.Reader
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path

internal class OpsCatalogTest {
    @Test fun shippedCatalogPreservesAospIdsIncludingTheUnusedSlot() {
        val file = Path.of(System.getProperty("doihave.projectDir"), "src/main/assets/appops.tsv")
        Files.newBufferedReader(file).use { reader ->
            val entries = OpsCatalog.read(reader)
            assertEquals(178, entries.size)
            val camera = entries.single { it.id == 26 }
            assertEquals("android:camera", camera.name)
            assertEquals("android.permission.CAMERA", camera.permission)
            assertFalse(entries.any { it.id == 96 })
            assertEquals(178, entries.last().id)
        }
    }
    @Test fun commentsBlankLinesAndEmptyPermissionsAreSupported() {
        assertEquals(listOf(OpsCatalog.Operation(3, "android:vibrate", "")), read("# AOSP\n\n3\tandroid:vibrate\t\n"))
    }
    @Test fun rejectsTruncatedRowsAndExtraColumns() {
        invalid("26\tandroid:camera")
        invalid("26\tandroid:camera\tandroid.permission.CAMERA\textra")
    }
    @Test fun rejectsInvalidIdsAndNames() {
        invalid("oops\tandroid:camera\t")
        invalid("-1\tandroid:camera\t")
        invalid("26\tcamera\t")
        invalid("26\tandroid:camera\tCAMERA")
    }
    @Test fun rejectsDuplicateIdsAndNames() {
        invalid("26\tandroid:camera\t\n26\tandroid:record_audio\t")
        invalid("26\tandroid:camera\t\n27\tandroid:camera\t")
    }
    @Test fun readErrorsArePropagated() {
        assertThrows(IOException::class.java) {
            OpsCatalog.read(object : Reader() {
                override fun read(buffer: CharArray, offset: Int, length: Int): Int = throw IOException("unavailable")
                override fun close() = Unit
            })
        }
    }
    private fun read(text: String) = OpsCatalog.read(StringReader(text))
    private fun invalid(text: String) {
        val error = assertThrows(IOException::class.java) { read(text) }
        assertTrue(error.message!!.contains("line"))
    }
}
