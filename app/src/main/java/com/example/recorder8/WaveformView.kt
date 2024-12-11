package com.example.recorder8

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View


class WaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val linePaint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val maxPoints = 100
    private val amplitudes = mutableListOf<Float>()

    fun addAmplitude(amplitude: Float) {
        if (amplitudes.size >= maxPoints) {
            amplitudes.removeAt(0)
        }
        amplitudes.add(amplitude)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (amplitudes.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val step = width / maxPoints

        val midY = height / 2
        var x = 0f
        for (amp in amplitudes) {
            val scaledHeight = (amp / 32767f) * (height / 2)
            canvas?.drawLine(x, midY - scaledHeight, x, midY + scaledHeight, linePaint)
            x += step
        }
    }
}