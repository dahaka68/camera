package com.example.dahaka.mycam.util.camera

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
import com.example.dahaka.mycam.ui.view.AutoFitTextureView
import com.example.dahaka.mycam.util.CAMERA_BACK
import com.example.dahaka.mycam.util.CAMERA_FRONT
import com.example.dahaka.mycam.util.FLASH_MODE_OFF
import com.example.dahaka.mycam.util.FLASH_MODE_TORCH
import com.google.android.gms.common.images.Size
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

class CameraSource(val context: Context, detector: Detector<*>?) : CameraDataSource {
    private val cameraLock = Any()
    private var camera: Camera? = null
    var cameraFacing = CAMERA_BACK
    private var rotation: Int = 0
    private lateinit var previewSize: Size
    private var requestedFps = 30.0f
    private val windowManager: WindowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private var flashMode = FLASH_MODE_OFF
    private lateinit var surfaceHolder: SurfaceHolder
    private var processingThread: Thread? = null
    private var frameProcessor: FrameProcessingRunnable? = null
    private val bytesToByteBuffer = HashMap<ByteArray, ByteBuffer>()
    private var requestedPreviewWidth = 1024
    private var requestedPreviewHeight = 768
    private lateinit var selectedPair: SizePair

    init {
        frameProcessor = FrameProcessingRunnable(detector)
    }

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
            frameProcessor?.release()
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
                parameters.setPictureSize(pictureSize.width, pictureSize.height)
                parameters.setPreviewSize(previewSize.width, previewSize.height)
                parameters.setPreviewFpsRange(previewFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                        previewFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX])
                parameters.previewFormat = ImageFormat.NV21
                setRotation(camera, parameters, requestedCameraId)
                if (parameters.supportedFlashModes != null) {
                    if (parameters.supportedFlashModes.contains(flashMode)) {
                        parameters.flashMode = flashMode
                    } else {
                        Log.i(TAG, "Camera flash mode: $flashMode is not supported on this device.")
                    }
                }
                if (cameraFacing == CAMERA_BACK) {
                    flashMode = parameters.flashMode
                }
                camera.parameters = parameters
                camera.setPreviewCallbackWithBuffer(CameraPreviewCallback())
                camera.addCallbackBuffer(createPreviewBuffer(previewSize))
                camera.addCallbackBuffer(createPreviewBuffer(previewSize))
                camera.addCallbackBuffer(createPreviewBuffer(previewSize))
                camera.addCallbackBuffer(createPreviewBuffer(previewSize))
                this.camera = camera
                this.camera?.setPreviewDisplay(this.surfaceHolder)
                this.camera?.startPreview()
                processingThread = Thread(frameProcessor)
                frameProcessor?.setActive(true)
                processingThread?.start()
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
            frameProcessor?.setActive(false)
            if (processingThread != null) {
                try {
                    // Wait for the thread to complete to ensure that we can't have multiple threads
                    // executing at the same time
                    processingThread?.join()
                } catch (e: InterruptedException) {
                    Log.d(TAG, "Frame processing thread interrupted on release.")
                }
                processingThread = null
            }
            bytesToByteBuffer.clear()

            if (camera != null) {
                camera?.stopPreview()
                camera?.setPreviewCallbackWithBuffer(null)
                try {
                    camera?.setPreviewTexture(null)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to clear camera preview: $e")
                }
                camera?.release()
                camera = null
            }
        }
    }

    fun doZoom(scale: Float): Int {
        synchronized(cameraLock) {
            if (camera == null) {
                return 0
            }
            val parameters = camera?.parameters
            val maxZoom = parameters?.maxZoom ?: 0
            var currentZoom = parameters?.zoom?.plus(1) ?: 0
            val newZoom: Float
            newZoom = if (scale > 1) {
                currentZoom + scale * (maxZoom / 10)
            } else {
                currentZoom * scale
            }
            currentZoom = Math.round(newZoom) - 1
            if (currentZoom < 0) {
                currentZoom = 0
            } else if (currentZoom > maxZoom) {
                currentZoom = maxZoom
            }
            parameters?.zoom = currentZoom
            camera?.parameters = parameters
            return currentZoom
        }
    }

    override fun takePicture(picCallback: PictureCallback) {
        synchronized(cameraLock) {
            if (camera != null) {
                if (cameraFacing == CAMERA_BACK) {
                    setFlash(flashMode)
                }
                val doneCallback = PictureDoneCallback()
                doneCallback.pictureCallback = picCallback
                camera?.takePicture(null, null, null, doneCallback)
            }
        }
    }

    fun setFlash(mode: String?): Boolean {
        synchronized(cameraLock) {
            if (camera != null && mode != null) {
                val parameters = camera?.parameters
                val supportedFlashModes = parameters?.supportedFlashModes
                if (supportedFlashModes != null && supportedFlashModes.contains(mode)) {
                    parameters.flashMode = mode
                    if (mode == FLASH_MODE_TORCH) {
                        parameters.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                        camera?.parameters = parameters
                        camera?.startPreview()
                    } else {
                        camera?.parameters = parameters
                    }
                    flashMode = mode
                    return true
                }
            }
            return false
        }
    }

    private inner class PictureDoneCallback : Camera.PictureCallback {
        var pictureCallback: PictureCallback? = null

        override fun onPictureTaken(data: ByteArray, camera: Camera) {
            if (pictureCallback != null) {
                val pic = BitmapFactory.decodeByteArray(data, 0, data.size)
                pictureCallback?.onPictureTaken(pic)
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
        if (buffer != null) {
            if (!buffer.hasArray() || !buffer.array()!!.contentEquals(byteArray)) {
                throw IllegalStateException("Failed to create valid buffer for camera source.")
            }
        }
        bytesToByteBuffer[byteArray] = buffer
        return byteArray
    }

    private inner class CameraPreviewCallback : Camera.PreviewCallback {
        override fun onPreviewFrame(data: ByteArray, camera: Camera) {
            frameProcessor?.setNextFrame(data, camera)
        }
    }

    private inner class FrameProcessingRunnable(private var detector: Detector<*>?) : Runnable {
        private val startTimeMillis = SystemClock.elapsedRealtime()
        // This lock guards all of the member variables below.
        private val lock = Object()
        private var active = true
        private var pendingTimeMillis: Long = 0
        private var pendingFrameId = 0
        private var pendingFrameData: ByteBuffer? = null

        internal fun release() {
            detector?.release()
            detector = null
        }

        internal fun setActive(active: Boolean) {
            synchronized(lock) {
                this.active = active
                lock.notifyAll()
            }
        }

        internal fun setNextFrame(data: ByteArray, camera: Camera) {
            synchronized(lock) {
                if (pendingFrameData != null) {
                    camera.addCallbackBuffer(pendingFrameData?.array())
                    pendingFrameData = null
                }
                if (!bytesToByteBuffer.containsKey(data)) {
                    Log.d(TAG, "Skipping frame. Could not find ByteBuffer associated with the image data from the camera.")
                    return
                }
                pendingTimeMillis = SystemClock.elapsedRealtime() - startTimeMillis
                pendingFrameId++
                pendingFrameData = bytesToByteBuffer[data]
                // Notify the processor thread if it is waiting on the next frame (see below).
                lock.notifyAll()
            }
        }

        override fun run() {
            var outputFrame: Frame? = null
            var data: ByteBuffer? = null
            while (true) {
                synchronized(lock) {
                    while (active && pendingFrameData == null) {
                        try {
                            // Wait for the next frame to be received from the camera, since we
                            // don't have it yet.
                            lock.wait()
                        } catch (e: InterruptedException) {
                            Log.d(TAG, "Frame processing loop terminated.", e)
                            return
                        }
                    }
                    if (!active) {
                        return
                    }

                    val previewW = previewSize.width
                    val previewH = previewSize.height
                    if (pendingFrameData != null) {
                        outputFrame = Frame.Builder()
                                .setImageData(quarterNV21(pendingFrameData!!, previewW, previewH),
                                        previewW / 4, previewH / 4, ImageFormat.NV21)
                                .setId(pendingFrameId)
                                .setTimestampMillis(pendingTimeMillis)
                                .setRotation(rotation)
                                .build()
                    }
                    data = pendingFrameData as ByteBuffer
                    pendingFrameData = null
                }
                try {
                    detector?.receiveFrame(outputFrame)
                } catch (t: Throwable) {
                    Log.e(TAG, "Exception thrown from receiver.", t)
                } finally {
                    camera?.addCallbackBuffer(data?.array())
                }
            }
        }
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
            if (pictureSize != null) {
                picture = Size(pictureSize.width, pictureSize.height)
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

    private fun quarterNV21(d: ByteBuffer, imageWidth: Int, imageHeight: Int): ByteBuffer {
        val data = d.array()
        val yuv = ByteArray(imageWidth / 4 * imageHeight / 4 * 3 / 2)
        var i = 0
        run {
            var y = 0
            while (y < imageHeight) {
                var x = 0
                while (x < imageWidth) {
                    yuv[i] = data[y * imageWidth + x]
                    i++
                    x += 4
                }
                y += 4
            }
        }
        // halve U and V color components
        var y = 0
        while (y < imageHeight / 2) {
            var x = 0
            while (x < imageWidth) {
                yuv[i] = data[imageWidth * imageHeight + y * imageWidth + x]
                i++
                yuv[i] = data[imageWidth * imageHeight + y * imageWidth + (x + 1)]
                i++
                x += 8
            }
            y += 4
        }
        //REDUCED TO QUARTER QUALITY AND ONLY IN GRAY SCALE!
        return ByteBuffer.wrap(yuv)
    }

    companion object {
        private const val TAG = "CameraSource"
        private const val ASPECT_RATIO_TOLERANCE = 0.01f
    }
}