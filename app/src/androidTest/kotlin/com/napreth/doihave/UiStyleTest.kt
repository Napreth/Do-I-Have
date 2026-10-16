package com.napreth.doihave

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.view.ContextThemeWrapper
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class UiStyleTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun chipSelectionRetainsRippleAndUpdatesColorsInBothThemes() {
        instrumentation.runOnMainSync {
            for (dark in listOf(false, true)) {
                val context = ContextThemeWrapper(instrumentation.targetContext,
                    if (dark) R.style.AppTheme_Dark else R.style.AppTheme)
                val style = UiStyle(context, dark)
                lateinit var chip: TextView
                chip = style.filterChip(R.string.all, false) { chip.isSelected = !chip.isSelected }
                val ripple = chip.background as RippleDrawable
                val fill = ripple.getDrawable(0) as StateListDrawable
                repeat(6) {
                    chip.performClick()
                    assertSame(ripple, chip.background)
                    assertEquals(if (chip.isSelected) (if (dark) style.background else Color.WHITE) else style.secondary,
                        chip.currentTextColor)
                    assertEquals(if (chip.isSelected) style.accent else Color.TRANSPARENT,
                        (fill.current as GradientDrawable).color!!.defaultColor)
                }
            }
        }
    }

    @Test fun screenFiltersKeepRippleAfterRepeatedSelections() {
        val activity = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            instrumentation.runOnMainSync {
                val filters = activity.findViewById<Ui.Flow>(R.id.state_filters)
                val backgrounds = (0 until filters.childCount).map { filters.getChildAt(it).background }
                for (selected in 0 until filters.childCount) {
                    filters.getChildAt(selected).performClick()
                    for (i in 0 until filters.childCount) {
                        val chip = filters.getChildAt(i)
                        assertEquals(i == selected, chip.isSelected)
                        assertTrue(chip.background is RippleDrawable)
                        assertSame(backgrounds[i], chip.background)
                    }
                }
            }
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
}
