package com.example.dahaka.mycam.ui.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.example.dahaka.mycam.util.CAMERA_BACK
import com.example.dahaka.mycam.util.CAMERA_FRONT
import java.util.*

class GraphicOverlay<T : GraphicOverlay.Graphic>(context: Context, attrs: AttributeSet? = null)
    : View(context, attrs) {
    private val lock = Any()
    private var previewWidth: Int = 0
    private var widthScaleFactor = 1.0f
    private var previewHeight: Int = 0
    private var heightScaleFactor = 1.0f
    private var cameraId = CAMERA_BACK
    private val mGraphics = HashSet<T>()

    val graphics: List<T>
        get() = synchronized(lock) {
            return Vector(mGraphics)
        }

    abstract class Graphic(private val overlay: GraphicOverlay<*>) {
        abstract fun draw(canvas: Canvas)
        fun scaleX(horizontal: Float): Float {
            return horizontal * overlay.widthScaleFactor
        }

        fun scaleY(vertical: Float): Float {
            return vertical * overlay.heightScaleFactor
        }

        fun translateX(x: Float): Float {
            return if (overlay.cameraId == CAMERA_FRONT) {
                overlay.width - scaleX(x)
            } else {
                scaleX(x)
            }
        }

        fun translateY(y: Float): Float {
            return scaleY(y)
        }

        fun postInvalidate() {
            overlay.postInvalidate()
        }
    }

    fun clear() {
        synchronized(lock) {
            mGraphics.clear()
        }
        postInvalidate()
    }

    fun add(graphic: T) {
        synchronized(lock) {
            mGraphics.add(graphic)
        }
        postInvalidate()
    }

    fun remove(graphic: T) {
        synchronized(lock) {
            mGraphics.remove(graphic)
        }
        postInvalidate()
    }

    fun setCameraInfo(previewWidth: Int, previewHeight: Int, facing: Int) {
        synchronized(lock) {
            this.previewWidth = previewWidth
            this.previewHeight = previewHeight
            cameraId = facing
        }
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        synchronized(lock) {
            if (previewWidth != 0 && previewHeight != 0) {
                widthScaleFactor = canvas.width.toFloat() / previewWidth.toFloat()
                heightScaleFactor = canvas.height.toFloat() / previewHeight.toFloat()
            }
            for (graphic in mGraphics) {
                graphic.draw(canvas)
            }
        }
    }
}
