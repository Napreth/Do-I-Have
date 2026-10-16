package com.napreth.doihave

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.view.ViewGroup
import kotlin.math.max
import kotlin.math.min

internal object Ui {
    // Programmatic views; these constructors are never used by XML inflation.
    @SuppressLint("ViewConstructor")
    class Flow(context: Context, private val gap: Int) : ViewGroup(context) {
        override fun onMeasure(widthSpec: Int, heightSpec: Int) {
            val width = MeasureSpec.getSize(widthSpec)
            var x = 0
            var y = 0
            var lineHeight = 0
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.visibility == GONE) continue
                measureChild(child, widthSpec, heightSpec)
                if (x > 0 && x + child.measuredWidth > width) {
                    x = 0
                    y += lineHeight + gap
                    lineHeight = 0
                }
                x += child.measuredWidth + gap
                lineHeight = max(lineHeight, child.measuredHeight)
            }
            setMeasuredDimension(resolveSize(width, widthSpec), resolveSize(y + lineHeight, heightSpec))
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            val width = right - left
            var x = 0
            var y = 0
            var lineHeight = 0
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.visibility == GONE) continue
                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight
                if (x > 0 && x + childWidth > width) {
                    x = 0
                    y += lineHeight + gap
                    lineHeight = 0
                }
                val childLeft = if (layoutDirection == LAYOUT_DIRECTION_RTL) width - x - childWidth else x
                child.layout(childLeft, y, childLeft + childWidth, y + childHeight)
                x += childWidth + gap
                lineHeight = max(lineHeight, childHeight)
            }
        }

        override fun generateDefaultLayoutParams() = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    @SuppressLint("ViewConstructor")
    class Icon(context: Context, name: String, color: Int) : View(context) {
        var name = name
            set(value) { if (field != value) { field = value; invalidate() } }
        private val path = Path()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 1.7f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
        private fun line(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) = canvas.drawLine(x1, y1, x2, y2, paint)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.save()
            val size = min(width, height).toFloat()
            canvas.translate((width - size) / 2, (height - size) / 2)
            canvas.scale(size / 24f, size / 24f)
            path.reset()
            when (name) {
                "check" -> {
                    path.moveTo(5f, 12f); path.lineTo(10f, 17f); path.lineTo(19f, 7f)
                    canvas.drawPath(path, paint)
                }
                "arrow" -> { line(canvas, 9f, 6f, 15f, 12f); line(canvas, 15f, 12f, 9f, 18f) }
                "close" -> { line(canvas, 6f, 6f, 18f, 18f); line(canvas, 6f, 18f, 18f, 6f) }
                "search" -> { canvas.drawCircle(10.5f, 10.5f, 6.5f, paint); line(canvas, 16f, 16f, 21f, 21f) }
                "ops" -> for (y in 6..18 step 6) {
                    canvas.drawCircle(5f, y.toFloat(), 1f, paint); line(canvas, 10f, y.toFloat(), 20f, y.toFloat())
                }
                "settings" -> for (x in 5..19 step 7) {
                    val y = if (x == 12) 15f else 8f
                    line(canvas, x.toFloat(), 3f, x.toFloat(), y - 3)
                    line(canvas, x.toFloat(), y + 3, x.toFloat(), 21f)
                    canvas.drawCircle(x.toFloat(), y, 3f, paint)
                }
                "globe" -> {
                    canvas.drawCircle(12f, 12f, 9f, paint)
                    canvas.drawOval(8f, 3f, 16f, 21f, paint)
                    line(canvas, 3f, 12f, 21f, 12f)
                }
                "theme" -> {
                    canvas.drawCircle(12f, 12f, 8.5f, paint)
                    paint.style = Paint.Style.FILL
                    canvas.drawArc(5f, 5f, 19f, 19f, 90f, 180f, true, paint)
                    paint.style = Paint.Style.STROKE
                }
                "code" -> {
                    line(canvas, 7f, 6f, 2f, 12f); line(canvas, 2f, 12f, 7f, 18f)
                    line(canvas, 17f, 6f, 22f, 12f); line(canvas, 22f, 12f, 17f, 18f)
                    line(canvas, 14f, 4f, 10f, 20f)
                }
                "info" -> { canvas.drawCircle(12f, 12f, 9f, paint); line(canvas, 12f, 11f, 12f, 17f); line(canvas, 12f, 7f, 12f, 7.2f) }
                else -> {
                    path.moveTo(12f, 2f); path.lineTo(21f, 6f); path.lineTo(21f, 12f)
                    path.cubicTo(21f, 17f, 17f, 21f, 12f, 23f)
                    path.cubicTo(7f, 21f, 3f, 17f, 3f, 12f); path.lineTo(3f, 6f); path.close()
                    canvas.drawPath(path, paint)
                    line(canvas, 8f, 12f, 11f, 15f); line(canvas, 11f, 15f, 17f, 9f)
                }
            }
            canvas.restore()
        }
    }
}
