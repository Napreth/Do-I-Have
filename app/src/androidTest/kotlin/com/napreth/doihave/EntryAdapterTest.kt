package com.napreth.doihave

import android.content.Context
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class EntryAdapterTest {
    private val prefix = "android.permission."
    private fun context(): Context = ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().targetContext, R.style.AppTheme)
    private fun onMain(assertions: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(assertions)
    private fun permission(name: String, label: String, granted: Boolean) =
        PermissionRepository.Entry(prefix + name, label, PermissionRepository.Kind.RUNTIME, PermissionState.permission(granted))

    @Test fun recycledPermissionRowsClearPreviousGrantAndText() = onMain {
        val context = context()
        val adapter = EntryAdapter(context, UiStyle(context, false), true)
        adapter.updatePermissions(listOf(permission("CAMERA", "Camera", true), permission("RECORD_AUDIO", "Microphone", false)), PermissionFilter.ALL, "")
        val parent = FrameLayout(context)
        val first = adapter.getView(0, null, parent) as LinearLayout
        val status = first.getChildAt(1)
        assertNotNull(status.background)
        val second = adapter.getView(1, first, parent) as LinearLayout
        assertSame("Adapter must reuse the row", first, second)
        assertEquals("Microphone", (second.getChildAt(0) as TextView).text.toString())
        assertTrue(second.contentDescription.toString().contains(prefix + "RECORD_AUDIO"))
        assertNull("Previous grant background must be cleared", status.background)
        adapter.getView(0, second, parent)
        assertNotNull("Grant decoration must also be restored", status.background)
    }

    @Test fun unknownRowsDoNotKeepARecycledCheckmark() = onMain {
        val context = context()
        val adapter = EntryAdapter(context, UiStyle(context, false), true)
        val unknown = PermissionRepository.Entry(prefix + "SCHEDULE_EXACT_ALARM", "Alarms", PermissionRepository.Kind.SPECIAL,
            PermissionState.exactAlarm(GrantState.DENIED, 3, true))
        adapter.updatePermissions(listOf(permission("CAMERA", "Camera", true), unknown), PermissionFilter.ALL, "")
        val parent = FrameLayout(context)
        val first = adapter.getView(0, null, parent)
        val second = adapter.getView(1, first, parent) as LinearLayout
        val status = second.getChildAt(1) as FrameLayout
        assertNull(status.background)
        assertEquals(View.GONE, status.getChildAt(0).visibility)
        assertEquals(View.VISIBLE, status.getChildAt(1).visibility)
        assertTrue(second.contentDescription.toString().contains(context.getString(R.string.grant_unknown)))
        adapter.getView(0, second, parent)
        assertEquals(View.VISIBLE, status.getChildAt(0).visibility)
        assertEquals(View.GONE, status.getChildAt(1).visibility)
    }

    @Test fun recycledOperationRowsReplaceNumberNameAndTrailingState() = onMain {
        val context = context()
        val adapter = EntryAdapter(context, UiStyle(context, true), false)
        adapter.updateOperations(listOf(OpsRepository.Entry(26, "android:camera", prefix + "CAMERA", 4, GrantState.GRANTED),
            OpsRepository.Entry(3, "android:vibrate", prefix + "VIBRATE", 3, GrantState.GRANTED)), OpsFilter.ALL, "")
        val parent = FrameLayout(context)
        val first = adapter.getView(0, null, parent) as LinearLayout
        val second = adapter.getView(1, first, parent) as LinearLayout
        assertSame(first, second)
        val details = second.getChildAt(0) as LinearLayout
        val heading = details.getChildAt(0) as LinearLayout
        assertEquals(context.getString(R.string.op_number, 3), (heading.getChildAt(0) as TextView).text.toString())
        assertEquals("VIBRATE", (heading.getChildAt(1) as TextView).text.toString())
        assertEquals("android:vibrate", (details.getChildAt(2) as TextView).text.toString())
        assertEquals(OpsRepository.modeLabel(context, 3), second.findViewById<TextView>(R.id.op_status).text.toString())
        assertFalse(second.contentDescription.toString().contains("camera"))
    }

    @Test fun englishUnknownStateFitsWithLargeText() = onMain {
        val base = context()
        val config = Configuration(base.resources.configuration).apply { fontScale = 2f; setLocale(Locale.ENGLISH) }
        val localized = ContextThemeWrapper(base.createConfigurationContext(config), R.style.AppTheme)
        val style = UiStyle(localized, false)
        val adapter = EntryAdapter(localized, style, true)
        adapter.updatePermissions(listOf(PermissionRepository.Entry(prefix + "SCHEDULE_EXACT_ALARM", "Alarms", PermissionRepository.Kind.SPECIAL,
            PermissionState.exactAlarm(GrantState.DENIED, 3, true))), PermissionFilter.ALL, "")
        val row = adapter.getView(0, null, FrameLayout(localized)) as LinearLayout
        row.measure(View.MeasureSpec.makeMeasureSpec(style.dp(300), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        val label = (row.getChildAt(1) as FrameLayout).getChildAt(1) as TextView
        assertEquals("Unknown", label.text.toString())
        assertTrue(label.measuredWidth >= label.paint.measureText(label.text.toString()) + label.paddingLeft + label.paddingRight)
        assertTrue(label.measuredHeight <= (row.getChildAt(1) as FrameLayout).measuredHeight)
    }

    @Test fun permissionSearchAndGrantFilterCompose() = onMain {
        val context = context()
        val adapter = EntryAdapter(context, UiStyle(context, false), true)
        val camera = permission("CAMERA", "相机", true)
        val microphone = permission("RECORD_AUDIO", "麦克风", false)
        val data = listOf(camera, microphone)
        adapter.updatePermissions(data, PermissionFilter.GRANTED, " camera ")
        assertEquals(1, adapter.count)
        assertSame(camera, adapter.permissionAt(0))
        adapter.updatePermissions(data, PermissionFilter.DENIED, "CAMERA")
        assertEquals(0, adapter.count)
        adapter.updatePermissions(data, PermissionFilter.DENIED, "麦克风")
        assertEquals(1, adapter.count)
        assertSame(microphone, adapter.permissionAt(0))
    }

    @Test fun operationSearchUsesNumberAndNameWithExactModeFilter() = onMain {
        val context = context()
        val adapter = EntryAdapter(context, UiStyle(context, false), false)
        val camera = OpsRepository.Entry(26, "android:camera", prefix + "CAMERA", 4, GrantState.GRANTED)
        val vibrate = OpsRepository.Entry(3, "android:vibrate", prefix + "VIBRATE", 0, GrantState.GRANTED)
        val data = listOf(camera, vibrate)
        adapter.updateOperations(data, OpsFilter.FOREGROUND, "26")
        assertEquals(1, adapter.count)
        assertSame(camera, adapter.operationAt(0))
        adapter.updateOperations(data, OpsFilter.ALLOWED, "CAMERA")
        assertEquals(0, adapter.count)
        adapter.updateOperations(data, OpsFilter.ALL, " VIBRATE ")
        assertSame(vibrate, adapter.operationAt(0))
    }
}
