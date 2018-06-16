package com.example.dahaka.mycam.ui.barcode.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.dahaka.mycam.ui.camera.util.common.GraphicOverlay
import com.google.android.gms.vision.barcode.Barcode

class BarcodeGraphic(overlay: GraphicOverlay<*>) : GraphicOverlay.Graphic(overlay) {
    private val rectPaint: Paint
    private val textPaint: Paint
    private lateinit var barcode: Barcode

    init {
        currentColorIndex = (currentColorIndex + 1) % COLOR_CHOICES.size
        rectPaint = Paint().apply {
            color = COLOR_CHOICES[currentColorIndex]
            style = Paint.Style.STROKE
            strokeWidth = 4.0f
        }
        textPaint = Paint().apply {
            color = COLOR_CHOICES[currentColorIndex]
            textSize = 36.0f
        }
    }

    fun updateItem(barcode: Barcode) {
        this.barcode = barcode
        postInvalidate()
    }

    override fun draw(canvas: Canvas) {
        val barcode = this.barcode
        val rect = RectF(barcode.boundingBox)
        rect.apply {
            left = translateX(rect.left)
            top = translateY(rect.top)
            right = translateX(rect.right)
            bottom = translateY(rect.bottom)
        }
        canvas.apply {
            drawRect(rect, rectPaint)
            drawText(barcode.rawValue, rect.left, rect.bottom, textPaint)
        }
    }

    companion object {
        private val COLOR_CHOICES = intArrayOf(Color.BLUE, Color.CYAN, Color.GREEN)
        private var currentColorIndex = 0
    }
}