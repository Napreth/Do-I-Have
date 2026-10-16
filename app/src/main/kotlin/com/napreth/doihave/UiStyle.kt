package com.napreth.doihave

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

internal class UiStyle(private val context: Context, val dark: Boolean) {
    val background = Color.parseColor(if (dark) "#111111" else "#F6F6F6")
    val surface = Color.parseColor(if (dark) "#1C1C1C" else "#FFFFFF")
    val foreground = Color.parseColor(if (dark) "#EEEEEE" else "#1B1B1B")
    val secondary = Color.parseColor(if (dark) "#A5A5A5" else "#6B6B6B")
    val accent = Color.parseColor(if (dark) "#E0E0E0" else "#292929")
    val tint = Color.parseColor(if (dark) "#303030" else "#E7E7E7")
    val outline = Color.parseColor(if (dark) "#343434" else "#E2E2E2")
    fun dp(size: Int) = (size * context.resources.displayMetrics.density).roundToInt()

    fun text(value: String, size: Int, color: Int, bold: Boolean) = TextView(context).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        fontFeatureSettings = "kern"
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        includeFontPadding = false
    }

    fun column() = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    fun row() = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
    fun shape(color: Int, radius: Int, border: Boolean) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (border) setStroke(dp(1), outline)
    }

    fun clickable(view: View, color: Int, radius: Int, action: () -> Unit) =
        clickable(view, shape(color, radius, false), radius, action)

    private fun clickable(view: View, content: Drawable, radius: Int, action: () -> Unit) {
        view.background = RippleDrawable(ColorStateList.valueOf(if (dark) 0x30FFFFFF else 0x16000000),
            content, shape(Color.WHITE, radius, false))
        view.setOnClickListener { action() }
        view.isFocusable = true
    }

    fun filterChip(label: Int, selected: Boolean, action: () -> Unit): TextView {
        val states = arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf())
        val fill = StateListDrawable().apply {
            addState(states[0], shape(accent, 24, false))
            addState(states[1], shape(Color.TRANSPARENT, 24, false))
        }
        return text(context.getString(label), 12, secondary, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(18), 0, dp(18), 0)
            setSingleLine(true)
            setTextColor(ColorStateList(states, intArrayOf(if (dark) this@UiStyle.background else Color.WHITE, secondary)))
            clickable(this, fill, 24, action)
            isSelected = selected
        }
    }

    fun addSpace(layout: LinearLayout, height: Int) =
        layout.addView(View(context), LinearLayout.LayoutParams(1, dp(height)))
}
