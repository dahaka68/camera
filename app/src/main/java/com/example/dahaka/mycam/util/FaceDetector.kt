package com.example.dahaka.mycam.util

import android.content.Context
import android.graphics.*
import android.widget.Toast
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.FaceDetector

class FaceDetector(val context: Context, path: String) {

    private val options = BitmapFactory.Options()
    private val rectPaint = Paint()
    private val bitmap = BitmapFactory.decodeFile(path, options)
    private val tempBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)
    private val tempCanvas = Canvas(tempBitmap)

    init {
        rectPaint.strokeWidth = 5f
        rectPaint.color = Color.RED
        rectPaint.style = Paint.Style.STROKE
        tempCanvas.drawBitmap(bitmap, 0f, 0f, null)
        createFaceDetector()
    }

    private fun createFaceDetector() {
        val faceDetector = FaceDetector.Builder(context).setTrackingEnabled(false).build()
        if (!faceDetector.isOperational) {
            Toast.makeText(context, "Cold not set up the Face Detector!", Toast.LENGTH_LONG).show()
            return
        }
        val frame = Frame.Builder().setBitmap(bitmap).build()
        val faces = faceDetector.detect(frame)

        for (i in 0 until faces.size()) {
            val thisFace = faces.valueAt(i)
            val x1 = thisFace.position.x
            val y1 = thisFace.position.y
            val x2 = x1 + thisFace.width
            val y2 = y1 + thisFace.height
            tempCanvas.drawRoundRect(RectF(x1, y1, x2, y2), 2f, 2f, rectPaint)
        }
    }

    fun getFaceImage(): Bitmap? = tempBitmap
}