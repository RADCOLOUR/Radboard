package com.radcolour.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class FretboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class ChordPosition(
        val startFret: Int,
        val strings: List<Int>,
        val frets: List<Int>,
        val barreFret: Int = -1,
        val barreFromString: Int = -1,
        val barreToString: Int = -1
    )

    private var position: ChordPosition? = null
    private var stringCount = 6

    private val boardPaint = Paint().apply {
        color = 0xFF1A1A1A.toInt()
        style = Paint.Style.FILL
    }

    private val linePaint = Paint().apply {
        color = 0xFF2B2B2B.toInt()
        strokeWidth = 1.5f
        isAntiAlias = true
    }

    private val nutPaint = Paint().apply {
        color = 0xFFE6E6E6.toInt()
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val dotPaint = Paint().apply {
        color = 0xFF7DD6FF.toInt()
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val barrePaint = Paint().apply {
        color = 0xFF7DD6FF.toInt()
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val mutedPaint = Paint().apply {
        color = 0xFFFF5449.toInt()
        strokeWidth = 2f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val openPaint = Paint().apply {
        color = 0xFF7DD6FF.toInt()
        strokeWidth = 2f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint().apply {
        color = 0xFF003549.toInt()
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val fretLabelPaint = Paint().apply {
        color = 0xFF8A8A8A.toInt()
        textSize = 18f
        isAntiAlias = true
        textAlign = Paint.Align.RIGHT
    }

    fun setPosition(pos: ChordPosition, strings: Int = 6) {
        this.position = pos
        this.stringCount = strings
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pos = position ?: return
        val padding = 40f
        val topPadding = 60f
        val bottomPadding = 20f
        val totalWidth = width.toFloat() - padding * 2
        val totalHeight = height.toFloat() - topPadding - bottomPadding
        val stringSpacing = totalWidth / (stringCount - 1)
        val fretCount = 5
        val fretSpacing = totalHeight / fretCount
        val dotRadius = stringSpacing * 0.35f
        val indicatorRadius = stringSpacing * 0.18f

        if (pos.startFret == 1) {
            canvas.drawLine(padding, topPadding, padding + totalWidth, topPadding, nutPaint)
        } else {
            canvas.drawLine(padding, topPadding, padding + totalWidth, topPadding, linePaint)
            canvas.drawText(
                "${pos.startFret}fr",
                padding - 8f,
                topPadding + fretSpacing / 2,
                fretLabelPaint
            )
        }

        for (i in 1..fretCount) {
            val y = topPadding + i * fretSpacing
            canvas.drawLine(padding, y, padding + totalWidth, y, linePaint)
        }

        for (i in 0 until stringCount) {
            val x = padding + i * stringSpacing
            canvas.drawLine(x, topPadding, x, topPadding + totalHeight, linePaint)
        }

        if (pos.barreFret >= 0 && pos.barreFromString >= 0 && pos.barreToString >= 0) {
            val fretIndex = pos.barreFret - pos.startFret
            val y = topPadding + fretIndex * fretSpacing + fretSpacing / 2
            val xStart = padding + pos.barreFromString * stringSpacing
            val xEnd = padding + pos.barreToString * stringSpacing
            val rect = RectF(
                xStart - dotRadius,
                y - dotRadius,
                xEnd + dotRadius,
                y + dotRadius
            )
            canvas.drawRoundRect(rect, dotRadius, dotRadius, barrePaint)
        }

        for (i in 0 until stringCount) {
            if (pos.strings[i] > 0) {
                if (pos.barreFret >= 0
                    && pos.frets[i] == pos.barreFret
                    && i >= pos.barreFromString
                    && i <= pos.barreToString) continue

                val x = padding + i * stringSpacing
                val fretIndex = pos.frets[i] - pos.startFret
                val y = topPadding + fretIndex * fretSpacing + fretSpacing / 2
                canvas.drawCircle(x, y, dotRadius, dotPaint)
                textPaint.textSize = dotRadius * 0.9f
                canvas.drawText(
                    pos.strings[i].toString(),
                    x,
                    y + textPaint.textSize / 3,
                    textPaint
                )
            }
        }

        for (i in 0 until stringCount) {
            val x = padding + i * stringSpacing
            val y = topPadding - indicatorRadius - 4f
            when (pos.strings[i]) {
                -1 -> canvas.drawCircle(x, y, indicatorRadius, openPaint)
                0 -> {
                    val offset = indicatorRadius * 0.7f
                    canvas.drawLine(x - offset, y - offset, x + offset, y + offset, mutedPaint)
                    canvas.drawLine(x + offset, y - offset, x - offset, y + offset, mutedPaint)
                }
            }
        }
    }
}