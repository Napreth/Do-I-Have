package com.napreth.doihave

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

internal class PermissionLabelsTest {
    @Test fun rejectsMissingAndRawFrameworkLabels() {
        listOf(null, "", "  ", "CAMERA", "MANAGE_MEDIA", "android.permission.MANAGE_MEDIA", "com.vendor.permission.CAMERA").forEach {
            assertFalse(it.toString(), PermissionLabels.isDisplayName(it, "android.permission.MANAGE_MEDIA"))
        }
    }
    @Test fun acceptsTranslatedHumanReadableLabels() {
        assertTrue(PermissionLabels.isDisplayName("访问所有文件", "android.permission.MANAGE_EXTERNAL_STORAGE"))
        assertTrue(PermissionLabels.isDisplayName("  All files access  ", "android.permission.MANAGE_EXTERNAL_STORAGE"))
    }
    @Test fun screenshotRegressionsHaveExplicitFallbacks() {
        assertEquals(R.string.permission_all_files, PermissionLabels.resourceFor("android.permission.MANAGE_EXTERNAL_STORAGE"))
        assertEquals(R.string.permission_manage_media, PermissionLabels.resourceFor("android.permission.MANAGE_MEDIA"))
        assertEquals(R.string.permission_usage, PermissionLabels.resourceFor("android.permission.PACKAGE_USAGE_STATS"))
        assertEquals(R.string.permission_handover, PermissionLabels.resourceFor("android.permission.ACCEPT_HANDOVER"))
        assertEquals(R.string.permission_voicemail, PermissionLabels.resourceFor("com.android.voicemail.permission.ADD_VOICEMAIL"))
        assertEquals(0, PermissionLabels.resourceFor("android.permission.UNKNOWN_NEW_PERMISSION"))
    }
    @Test fun unknownNamesHaveReadableLastResortFallbacks() {
        assertEquals("Future sensor access", PermissionLabels.humanize("android.permission.FUTURE_SENSOR_ACCESS"))
        assertEquals("Vendor action", PermissionLabels.humanize("com.vendor.permission.VENDOR_ACTION"))
    }
    @Test fun chineseAndEnglishFallbackCatalogsStayCompleteAndReadable() {
        val english = resources("values")
        val chinese = resources("values-zh")
        assertTrue(english.size >= 50)
        assertEquals(english.keys, chinese.keys)
        english.forEach { (key, value) ->
            assertTrue(key, PermissionLabels.isDisplayName(value, key))
            assertTrue(key, PermissionLabels.isDisplayName(chinese.getValue(key), key))
            assertTrue("Chinese translation missing for $key", chinese.getValue(key).any { it in '\u4e00'..'\u9fff' })
        }
    }

    private fun resources(locale: String): Map<String, String> {
        val file = Path.of(System.getProperty("doihave.projectDir"), "src/main/res", locale, "permission_names.xml")
        val factory = DocumentBuilderFactory.newInstance().apply { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        val strings = factory.newDocumentBuilder().parse(file.toFile()).getElementsByTagName("string")
        return buildMap {
            for (i in 0 until strings.length) {
                val node = strings.item(i)
                val key = node.attributes.getNamedItem("name").nodeValue
                assertNull("Duplicate resource $key", put(key, node.textContent))
            }
        }
    }
}
