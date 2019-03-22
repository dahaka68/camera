package com.example.dahaka.mycam.ui.camera.util.camera1

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager
import android.widget.Toast
import com.example.dahaka.mycam.ui.camera.util.camera2.AutoFitTextureView
import com.example.dahaka.mycam.ui.camera.util.common.CameraDataSource
import com.example.dahaka.mycam.util.CAMERA_BACK
import com.example.dahaka.mycam.util.CAMERA_FRONT
import com.example.dahaka.mycam.util.FLASH_MODE_OFF
import com.example.dahaka.mycam.util.FLASH_MODE_TORCH
import com.example.dahaka.mycam.ui.camera.util.common.PictureCallback
import com.google.android.gms.common.images.Size
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

class CameraSource(val context: Context) : CameraDataSource {
    private val cameraLock = Any()
    private var camera: Camera? = null
    var cameraFacing = CAMERA_BACK
    private var rotation: Int = 0
    private lateinit var previewSize: Size
    private var requestedFps = 30.0f
    private val windowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private var flashMode = FLASH_MODE_OFF
    private lateinit var surfaceHolder: SurfaceHolder
    private val bytesToByteBuffer = HashMap<ByteArray, ByteBuffer>()
    private var requestedPreviewWidth = 1024
    private var requestedPreviewHeight = 768
    private lateinit var selectedPair: SizePair

    override fun setFlashMode(flashMode: String) {
        this.flashMode = flashMode
    }

    fun setCamera(facing: Int) {
        if (facing != CAMERA_BACK && facing != CAMERA_FRONT) {
            throw IllegalArgumentException("Invalid camera: $facing")
        }
        cameraFacing = facing
    }

    fun getCameraPreviewSize(): Size? {
        return if (this::previewSize.isInitialized) {
            previewSize
        } else {
            null
        }
    }

    override fun release() {
        synchronized(cameraLock) {
            stop()
        }
    }

    @Throws(IOException::class)
    override fun start(surfaceHolder: SurfaceHolder) {
        synchronized(cameraLock) {
            if (camera != null) {
                return
            }
            try {
                this.surfaceHolder = surfaceHolder
                val requestedCameraId = getIdForRequestedCamera(cameraFacing)
                if (requestedCameraId == -1) {
                    throw RuntimeException("Could not find requested camera.")
                }
                val camera = Camera.open(requestedCameraId)
                val sizePair = selectSizePair(camera, requestedPreviewWidth, requestedPreviewHeight)
                val pictureSize = sizePair.pictureSize() ?: Size(1024, 768)
                previewSize = sizePair.previewSize()
                val previewFpsRange = selectPreviewFpsRange(camera, requestedFps)
                        ?: throw RuntimeException("Could not find suitable preview frames per second range.")
                val parameters = camera.parameters
                if (parameters.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                } else {
                    Log.i(TAG, "Camera focus mode: focusMode is not supported on this device.")
                }
                parameters.apply {
                    setPictureSize(pictureSize.width, pictureSize.height)
                    setPreviewSize(previewSize.width, previewSize.height)
                    setPreviewFpsRange(previewFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                            previewFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX])
                    previewFormat = ImageFormat.NV21
                    setRotation(camera, this, requestedCameraId)
                }
                camera.apply {
                    this.parameters = parameters
                    addCallbackBuffer(createPreviewBuffer(previewSize))
                    addCallbackBuffer(createPreviewBuffer(previewSize))
                    addCallbackBuffer(createPreviewBuffer(previewSize))
                    addCallbackBuffer(createPreviewBuffer(previewSize))
                }
                this.camera = camera
                this.camera?.setPreviewDisplay(this.surfaceHolder)
                this.camera?.startPreview()
            } catch (e: RuntimeException) {
                e.printStackTrace()
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun start(textureView: AutoFitTextureView, displayOrientation: Int) {
        //Not used
    }

    override fun stop() {
        synchronized(cameraLock) {
            bytesToByteBuffer.clear()

            camera?.let {
                it.stopPreview()
                it.setPreviewCallbackWithBuffer(null)
                try {
                    it.setPreviewTexture(null)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to clear camera preview: $e")
                }
                it.release()
                camera = null
            }
        }
    }

    override fun takePicture(picCallback: PictureCallback) {
        synchronized(cameraLock) {
            camera?.let {
                val doneCallback = PictureDoneCallback()
                doneCallback.pictureCallback = picCallback
                it.takePicture(null, null, null, doneCallback)
            }
        }
    }

    private inner class PictureDoneCallback : Camera.PictureCallback {
        var pictureCallback: PictureCallback? = null

        override fun onPictureTaken(data: ByteArray, camera: Camera) {
            pictureCallback?.let {
                val pic = BitmapFactory.decodeByteArray(data, 0, data.size)
                it.onPictureTaken(pic)
            }
            synchronized(cameraLock) {
                this@CameraSource.camera?.startPreview()
            }
        }
    }

    private fun selectPreviewFpsRange(camera: Camera, desiredPreviewFps: Float): IntArray? {
        val desiredPreviewFpsScaled = (desiredPreviewFps * 1000.0f).toInt()
        var selectedFpsRange: IntArray? = null
        var minDiff = Integer.MAX_VALUE
        val previewFpsRangeList = camera.parameters.supportedPreviewFpsRange
        for (range in previewFpsRangeList) {
            val deltaMin = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]
            val deltaMax = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]
            val diff = Math.abs(deltaMin) + Math.abs(deltaMax)
            if (diff < minDiff) {
                selectedFpsRange = range
                minDiff = diff
            }
        }
        return selectedFpsRange
    }

    private fun setRotation(camera: Camera, parameters: Camera.Parameters, cameraId: Int) {
        var degrees = 0
        when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
            else -> Log.e(TAG, "Bad rotation value")
        }
        val cameraInfo = CameraInfo()
        Camera.getCameraInfo(cameraId, cameraInfo)
        val angle: Int
        val displayAngle: Int
        if (cameraInfo.facing == CAMERA_FRONT) {
            angle = (cameraInfo.orientation + degrees) % 360
            displayAngle = (360 - angle) % 360 // compensate for it being mirrored
        } else {  // back-facing
            angle = (cameraInfo.orientation - degrees + 360) % 360
            displayAngle = angle
        }
        rotation = angle / 90
        camera.setDisplayOrientation(displayAngle)
        parameters.setRotation(angle)
    }

    private fun createPreviewBuffer(previewSize: Size): ByteArray {
        val bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21)
        val sizeInBits = (previewSize.height * previewSize.width * bitsPerPixel).toLong()
        val bufferSize = Math.ceil(sizeInBits / 8.0).toInt() + 1
        val byteArray = ByteArray(bufferSize)
        val buffer = ByteBuffer.wrap(byteArray)
        buffer.array()?.let {
            if (!it.contentEquals(byteArray)) {
                throw IllegalStateException("Failed to create valid buffer for camera source.")
            }
        }
        bytesToByteBuffer[byteArray] = buffer
        return byteArray
    }

    private fun getIdForRequestedCamera(facing: Int): Int {
        val cameraInfo = CameraInfo()
        for (i in 0 until Camera.getNumberOfCameras()) {
            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == facing) {
                return i
            }
        }
        return -1
    }

    private fun selectSizePair(camera: Camera, desiredWidth: Int, desiredHeight: Int): SizePair {
        val validPreviewSizes = generateValidPreviewSizeList(camera)
        var minDiff = Integer.MAX_VALUE
        for (sizePair in validPreviewSizes) {
            val size = sizePair.previewSize()
            val diff = Math.abs(size.width - desiredWidth) + Math.abs(size.height - desiredHeight)
            if (diff < minDiff) {
                selectedPair = sizePair
                minDiff = diff
            }
        }
        return selectedPair
    }

    class SizePair(previewSize: Camera.Size,
                   pictureSize: Camera.Size?) {
        private val preview: Size = Size(previewSize.width, previewSize.height)
        private var picture: Size? = null

        init {
            pictureSize?.let { size ->
                picture = Size(size.width, size.height)
            }
        }

        fun previewSize(): Size {
            return preview
        }

        fun pictureSize(): Size? {
            return picture
        }
    }

    private fun generateValidPreviewSizeList(camera: Camera): List<SizePair> {
        val parameters = camera.parameters
        val supportedPreviewSizes = parameters.supportedPreviewSizes
        val supportedPictureSizes = parameters.supportedPictureSizes
        val validPreviewSizes = ArrayList<SizePair>()
        for (previewSize in supportedPreviewSizes) {
            val previewAspectRatio = previewSize.width.toFloat() / previewSize.height.toFloat()
            for (pictureSize in supportedPictureSizes) {
                val pictureAspectRatio = pictureSize.width.toFloat() / pictureSize.height.toFloat()
                if (Math.abs(previewAspectRatio - pictureAspectRatio) < ASPECT_RATIO_TOLERANCE) {
                    validPreviewSizes.add(SizePair(previewSize, pictureSize))
                    break
                }
            }
        }
        if (validPreviewSizes.size == 0) {
            Log.w(TAG, "No preview sizes have a corresponding same-aspect-ratio picture size")
            for (previewSize in supportedPreviewSizes) {
                validPreviewSizes.add(SizePair(previewSize, null))
            }
        }
        return validPreviewSizes
    }

    companion object {
        private const val TAG = "CameraSource"
        private const val ASPECT_RATIO_TOLERANCE = 0.01f
    }
}