package com.example.dahaka.mycam.ui.camera.util.common

import android.content.Context
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.*
import com.example.dahaka.mycam.ui.camera.util.camera2.AutoFitTextureView
import com.example.dahaka.mycam.ui.barcode.util.BarcodeGraphic
import com.example.dahaka.mycam.ui.camera.util.camera1.CameraSource
import com.example.dahaka.mycam.ui.camera.util.camera2.Camera2Source
import java.io.IOException

class CameraSourcePreview
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : ViewGroup(context, attrs, defStyle) {
    private val surfaceView = SurfaceView(context)
    private val autoFitTextureView = AutoFitTextureView(context)
    private var usingCameraOne: Boolean = false
    private var startRequested: Boolean = false
    private var surfaceAvailable: Boolean = false
    private var viewAdded = false
    private val windowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private val size = Point()
    private val windowDisplay = windowManager.defaultDisplay
    private var cameraSource: CameraDataSource? = null
    private var overlay: GraphicOverlay<*>? = null
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var screenRotation: Int = 0
    private var isBarcode = false

    private val surfaceViewListener = object : SurfaceHolder.Callback {
        override fun surfaceCreated(surface: SurfaceHolder) {
            surfaceAvailable = true
            overlay?.bringToFront()
            try {
                startIfReady()
            } catch (e: IOException) {
                Log.e(TAG, "Could not start camera source.", e)
            }
        }

        override fun surfaceDestroyed(surface: SurfaceHolder) {
            surfaceAvailable = false
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            Log.e(TAG, "Surface changed!")
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            surfaceAvailable = true
            overlay?.bringToFront()
            try {
                startIfReady()
            } catch (e: IOException) {
                Log.e(TAG, "Could not start camera source.", e)
            }
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            Log.e(TAG, "SurfaceTexture changed!")
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            surfaceAvailable = false
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
    }

    init {
        screenHeight = getScreenHeight()
        screenWidth = getScreenWidth()
        screenRotation = windowDisplay.rotation
        startRequested = false
        surfaceAvailable = false
        surfaceView.holder.addCallback(surfaceViewListener)
        autoFitTextureView.surfaceTextureListener = surfaceTextureListener
    }

    @Throws(IOException::class)
    fun start(cameraSource: CameraDataSource?, overlay: GraphicOverlay<GraphicOverlay.Graphic>) {
        this.cameraSource = cameraSource
        usingCameraOne = cameraSource is CameraSource
        isBarcode = false
        this.overlay = overlay
        start(cameraSource)
    }

    @Throws(IOException::class)
    fun startBarcode(cameraSource: CameraSource?, overlay: GraphicOverlay<BarcodeGraphic>) {
        isBarcode = true
        this.cameraSource = cameraSource
        usingCameraOne = true
        this.overlay = overlay
        start(cameraSource)
    }

    @Throws(IOException::class)
    private fun start(cameraSource: CameraDataSource?) {
        if (cameraSource == null) {
            stop()
        }
        if (this.cameraSource != null) {
            startRequested = true
            if (!viewAdded) {
                if (cameraSource is CameraSource) {
                    addView(surfaceView)
                } else {
                    addView(autoFitTextureView)
                }
                viewAdded = true
            }
            try {
                startIfReady()
            } catch (e: IOException) {
                Log.e(TAG, "Could not start camera source.", e)
            }
        }
    }

    private fun getScreenHeight(): Int {
        windowDisplay.getSize(size)
        return size.y
    }

    private fun getScreenWidth(): Int {
        windowDisplay.getSize(size)
        return size.x
    }

    fun stop() {
        startRequested = false
        if (usingCameraOne) {
            cameraSource?.stop()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cameraSource?.stop()
            }
        }
    }

    fun release() {
        cameraSource?.release()
        cameraSource = null
    }

    @Throws(IOException::class)
    private fun startIfReady() {
        if (startRequested && surfaceAvailable) {
            try {
                if (usingCameraOne) {
                    cameraSource?.start(surfaceView.holder)
                    if (overlay != null) {
                        val size = (cameraSource as CameraSource).getCameraPreviewSize()
                        if (size != null) {
                            val min = Math.min(size.width, size.height)
                            val max = Math.max(size.width, size.height)
                            overlay?.setCameraInfo(min / 4, max / 4,
                                    (cameraSource as CameraSource).cameraFacing)
                            overlay?.clear()
                        } else {
                            stop()
                        }
                    }
                    startRequested = false
                } else {
                    cameraSource?.start(autoFitTextureView, screenRotation)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        cameraSource?.start(autoFitTextureView, screenRotation)
                        if (overlay != null) {
                            val size = (cameraSource as Camera2Source).getCameraPreviewSize()
                            if (size != null) {
                                val min = Math.min(size.width, size.height)
                                val max = Math.max(size.width, size.height)
                                overlay?.setCameraInfo(min / 4, max / 4,
                                        (cameraSource as Camera2Source).cameraFacing)
                                overlay?.clear()
                            } else {
                                stop()
                            }
                        }
                        startRequested = false
                    }
                }
            } catch (e: SecurityException) {
                Log.d(TAG, "SECURITY EXCEPTION: $e")
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var width = 720
        var height = 1280
        if (usingCameraOne) {
            val size = (cameraSource as CameraSource).getCameraPreviewSize()
            if (size != null) {
                height = size.width
                width = size.height
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val size = (cameraSource as Camera2Source).getCameraPreviewSize()
                if (size != null) {
                    height = size.width
                    width = size.height
                }
            }
        }
        val newWidth = height * screenWidth / screenHeight
        val layoutWidth = right - left
        val layoutHeight = bottom - top
        var childWidth = layoutWidth
        val aspectHeight = childWidth / 9 * 16
        var childHeight =
                if (layoutHeight < aspectHeight) { //check software buttons
                    (layoutWidth.toFloat() / newWidth.toFloat() * height).toInt() / 100 * 84
                } else {
                    if (isBarcode) {
                        (layoutWidth.toFloat() / newWidth.toFloat() * height).toInt()
                    } else {
                        (layoutWidth.toFloat() / newWidth.toFloat() * height).toInt() / 100 * 75 //aspect ratio 4:3
                    }
                }
        if (childHeight > layoutHeight) {
            childHeight = layoutHeight
            childWidth = (layoutHeight.toFloat() / height.toFloat() * newWidth).toInt()
        }
        for (i in 0 until childCount) {
            getChildAt(i).layout(0, 0, childWidth, childHeight)
        }
        try {
            startIfReady()
        } catch (e: IOException) {
            Log.e(TAG, "Could not start camera source.", e)
        }
    }

    companion object {
        private const val TAG = "CameraSourcePreview"
    }
}