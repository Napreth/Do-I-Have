package com.napreth.doihave

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class FlowLayoutTest {
    @Test fun wrapsRowsAndHonorsRightToLeftLayout() = onMain {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val flow = Ui.Flow(context, 10).apply { layoutDirection = View.LAYOUT_DIRECTION_RTL }
        repeat(3) { flow.addView(View(context), ViewGroup.LayoutParams(60, 20)) }
        layout(flow, 130)
        assertEquals(130, flow.getChildAt(0).right)
        assertEquals(0, flow.getChildAt(1).left)
        assertEquals(30, flow.getChildAt(2).top)
        assertEquals(50, flow.measuredHeight)
    }

    @Test fun allSevenStatesFitOnANarrowScreenWithLargeText() = onMain {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(base.resources.configuration).apply { fontScale = 2f }
        val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.AppTheme)
        val style = UiStyle(context, false)
        val flow = Ui.Flow(context, style.dp(8))
        for (label in OpsFilter.labels()) {
            val chip = style.text(context.getString(label), 12, style.secondary, false).apply {
                setSingleLine(true)
                setPadding(style.dp(18), 0, style.dp(18), 0)
            }
            flow.addView(chip, ViewGroup.LayoutParams(-2, style.dp(48)))
        }
        val width = style.dp(280)
        layout(flow, width)
        assertEquals(7, flow.childCount)
        assertTrue(flow.getChildAt(6).top > 0)
        for (i in 0 until flow.childCount) {
            val chip = flow.getChildAt(i)
            assertTrue(chip.left >= 0 && chip.right <= width)
            assertTrue(chip.bottom <= flow.measuredHeight)
        }
    }

    private fun onMain(assertions: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(assertions)
    private fun layout(flow: Ui.Flow, width: Int) {
        flow.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        flow.layout(0, 0, width, flow.measuredHeight)
    }
}
