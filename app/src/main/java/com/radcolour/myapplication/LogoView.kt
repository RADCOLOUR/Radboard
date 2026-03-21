package com.radcolour.myapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val pinkPaint = Paint().apply {
        color = 0xFFFFB3D9.toInt()
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val bluePaint = Paint().apply {
        color = 0xFF7DD6FF.toInt()
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val greenPaint = Paint().apply {
        color = 0xFFBFFFAA.toInt()
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val yellowPaint = Paint().apply {
        color = 0xFFFFE57A.toInt()
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val whitePaint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint().apply {
        color = 0x33000000.toInt()
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val size = minOf(width, height) * 0.45f

        // Outer hexagon — blue
        drawHexagon(canvas, cx, cy, size, 0f, bluePaint)
        drawHexagon(canvas, cx, cy, size, 0f, strokePaint)

        // Inner hexagon — rotated, pink
        drawHexagon(canvas, cx, cy, size * 0.7f, 30f, pinkPaint)
        drawHexagon(canvas, cx, cy, size * 0.7f, 30f, strokePaint)

        // Four corner circles — one per colour
        val circleOffset = size * 0.62f
        val circleRadius = size * 0.22f

        // Top — yellow
        canvas.drawCircle(cx, cy - circleOffset, circleRadius, yellowPaint)
        canvas.drawCircle(cx, cy - circleOffset, circleRadius, strokePaint)

        // Right — green
        canvas.drawCircle(cx + circleOffset * 0.866f, cy + circleOffset * 0.5f, circleRadius, greenPaint)
        canvas.drawCircle(cx + circleOffset * 0.866f, cy + circleOffset * 0.5f, circleRadius, strokePaint)

        // Bottom left — pink
        canvas.drawCircle(cx - circleOffset * 0.866f, cy + circleOffset * 0.5f, circleRadius, pinkPaint)
        canvas.drawCircle(cx - circleOffset * 0.866f, cy + circleOffset * 0.5f, circleRadius, strokePaint)

        // Centre circle — white
        canvas.drawCircle(cx, cy, size * 0.18f, whitePaint)
        canvas.drawCircle(cx, cy, size * 0.18f, strokePaint)

        // Inner triangle — blue, pointing up
        drawTriangle(canvas, cx, cy, size * 0.35f, bluePaint)
        drawTriangle(canvas, cx, cy, size * 0.35f, strokePaint)
    }

    private fun drawHexagon(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        rotationDeg: Float,
        paint: Paint
    ) {
        val path = Path()
        for (i in 0 until 6) {
            val angle = Math.toRadians((60 * i + rotationDeg).toDouble())
            val x = cx + radius * cos(angle).toFloat()
            val y = cy + radius * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawTriangle(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        paint: Paint
    ) {
        val path = Path()
        for (i in 0 until 3) {
            val angle = Math.toRadians((120 * i - 90.0))
            val x = cx + radius * cos(angle).toFloat()
            val y = cy + radius * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }
}