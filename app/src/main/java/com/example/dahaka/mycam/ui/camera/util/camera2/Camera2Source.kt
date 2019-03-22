package com.example.dahaka.mycam.ui.camera.util.camera2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import com.example.dahaka.mycam.ui.camera.util.common.CameraDataSource
import com.example.dahaka.mycam.util.*
import com.example.dahaka.mycam.ui.camera.util.common.PictureCallback
import java.io.IOException
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2Source(val context: Context) : CameraDataSource {
    private val cameraManager by lazy { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private val windowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private val cameraOpenCloseLock by lazy { Semaphore(1) }
    private val cameraThread by lazy { CameraThread() }
    private lateinit var textureView: AutoFitTextureView
    private val previewRequestBuilder by lazy {
        cameraDevice?.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
        )
    }
    private lateinit var previewRequest: CaptureRequest
    private lateinit var characteristics: CameraCharacteristics
    private var cameraId = "0"
    private var captureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private var state = STATE_PREVIEW
    private var flashSupported = false
    private var sensorOrientation = 0
    private var displayOrientation: Int = 0
    private var imageReader: ImageReader? = null
    private var cameraStarted = false
    var cameraFacing = CAMERA_BACK
    private lateinit var previewSize: Size

    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@Camera2Source.cameraDevice = cameraDevice
            createCameraPreviewSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@Camera2Source.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            onDisconnected(cameraDevice)
        }
    }

    private val onImageAvailableListener = PictureDoneCallback()
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        private fun process(result: CaptureResult) {
            when (state) {
                STATE_PREVIEW -> {
                }// Do nothing when the camera preview is working normally.
                STATE_WAITING_LOCK -> capturePicture(result)
                STATE_WAITING_PRE_CAPTURE -> {
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        state = STATE_WAITING_NON_PRE_CAPTURE
                    }
                }
                STATE_WAITING_NON_PRE_CAPTURE -> {
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        state = STATE_PICTURE_TAKEN
                        captureStillPicture()
                    }
                }
            }
        }

        private fun capturePicture(result: CaptureResult) {
            val afState = result.get(CaptureResult.CONTROL_AF_STATE)
            if (afState == null) {
                captureStillPicture()
            } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState
                    || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState
                    || CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED == afState
                    || CaptureRequest.CONTROL_AF_STATE_PASSIVE_FOCUSED == afState
                    || CaptureRequest.CONTROL_AF_STATE_INACTIVE == afState) {
                // CONTROL_AE_STATE can be null on some devices
                val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    state = STATE_PICTURE_TAKEN
                    captureStillPicture()
                } else {
                    runPreCaptureSequence()
                }
            }
        }

        override fun onCaptureProgressed(session: CameraCaptureSession, request: CaptureRequest,
                                         partialResult: CaptureResult) {
            process(partialResult)
        }

        override fun onCaptureCompleted(session: CameraCaptureSession,
                                        request: CaptureRequest,
                                        result: TotalCaptureResult) {
            process(result)
        }

        override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
            if (request.tag == FOCUS_TAG) {
                Log.d(TAG, "Manual AF failure: $failure")
            }
        }
    }

    fun getCameraPreviewSize(): Size? {
        return if (this::previewSize.isInitialized) {
            previewSize
        } else {
            null
        }
    }

    private fun setUpCameraOutputs(width: Int, height: Int) {
        try {
            if (cameraId in cameraManager.cameraIdList) {
                cameraId = cameraManager.cameraIdList[cameraFacing]
                characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

                val sizes = map.getOutputSizes(ImageFormat.JPEG)
                val largest = Collections.max(
                        Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),
                        CompareSizesByArea()
//                        compareBy { it.height == it.width / 16 * 9 }
                ) ?: Size(1920, 1080)
                imageReader = ImageReader.newInstance(largest.width, largest.height,
                        ImageFormat.JPEG, 2).apply {
                    setOnImageAvailableListener(onImageAvailableListener, cameraThread.backgroundHandler)
                }
                sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
                val rotation = windowManager.defaultDisplay.rotation
                val swappedDimensions = areDimensionsSwapped(rotation)
                val displaySize = Point()
                windowManager.defaultDisplay.getSize(displaySize)
                val rotatedPreviewWidth = if (swappedDimensions) height else width
                val rotatedPreviewHeight = if (swappedDimensions) width else height
                var maxPreviewWidth = if (swappedDimensions) displaySize.y else displaySize.x
                var maxPreviewHeight = if (swappedDimensions) displaySize.x else displaySize.y
                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) maxPreviewWidth = MAX_PREVIEW_WIDTH
                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) maxPreviewHeight = MAX_PREVIEW_HEIGHT
                previewSize = chooseOptimalSize(
                        map.getOutputSizes(SurfaceTexture::class.java),
                        rotatedPreviewWidth,
                        rotatedPreviewHeight,
                        maxPreviewWidth,
                        maxPreviewHeight,
                        largest
                ) ?: Size(1920, 1080)
                if (displayOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(previewSize.width, previewSize.height)
                } else {
                    textureView.setAspectRatio(previewSize.height, previewSize.width)
                }
                // Check if the flash is supported.
                flashSupported = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

                return
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun areDimensionsSwapped(displayRotation: Int): Boolean {
        var swappedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true
                }
            }
            else -> {
                Log.e(TAG, "Display rotation is invalid: $displayRotation")
            }
        }
        return swappedDimensions
    }

    private fun openCamera(width: Int, height: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permission = this.let {
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            }
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return
            }
            val storagePerm = this.let {
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (storagePerm != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        setUpCameraOutputs(width, height)
        configureTransform(width, height)
        try {
            cameraManager.openCamera(cameraId, stateCallback, cameraThread.backgroundHandler)
            // Wait for camera to open - 2.5 seconds is sufficient
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    private fun createCameraPreviewSession() {
        try {
            val texture = textureView.surfaceTexture
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)
            // This is the output Surface we need to start preview.
            val surface = Surface(texture)
            previewRequestBuilder?.addTarget(surface)
            cameraDevice?.createCaptureSession(Arrays.asList(surface, imageReader?.surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            if (cameraDevice == null) return
                            // When the session is ready, we start displaying the preview.
                            captureSession = cameraCaptureSession
                            try {
                                previewRequestBuilder?.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                setFlash(previewRequestBuilder)
                                previewRequestBuilder?.build()?.let { previewRequest = it }
                                captureSession?.setRepeatingRequest(previewRequest,
                                        captureCallback, cameraThread.backgroundHandler)
                            } catch (e: CameraAccessException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.d(TAG, "Camera Configuration failed!")
                        }
                    }, null)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == displayOrientation || Surface.ROTATION_270 == displayOrientation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale = Math.max(
                    viewHeight.toFloat() / previewSize.height,
                    viewWidth.toFloat() / previewSize.width)
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (displayOrientation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == displayOrientation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView.setTransform(matrix)
    }

    private fun lockFocus() {
        try {
            previewRequestBuilder?.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
            state = STATE_WAITING_LOCK
            captureSession?.capture(previewRequestBuilder?.build(), captureCallback, cameraThread.backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    fun runPreCaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            previewRequestBuilder?.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)
            state = STATE_WAITING_PRE_CAPTURE
            captureSession?.capture(previewRequestBuilder?.build(), captureCallback,
                    cameraThread.backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun captureStillPicture() {
        try {
            if (cameraDevice == null) return
            val captureBuilder = cameraDevice?.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE)?.apply {
                addTarget(imageReader?.surface)
                set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(characteristics, displayOrientation))
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }?.also { setFlash(it) }
            captureSession?.apply {
                capture(captureBuilder?.build(), null, null)
                abortCaptures()
            }
            unlockFocus()
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun setFlash(requestBuilder: CaptureRequest.Builder?) {
        requestBuilder?.set(CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF)
    }

    private fun getJpegOrientation(c: CameraCharacteristics, deviceOrientation: Int): Int {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0
        val sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        // Round device orientation to a multiple of 90
        var tempDeviceOrientation = (deviceOrientation + 45) / 90 * 90
        // Reverse device orientation for front-facing cameras
        val facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        if (facingFront) {
            tempDeviceOrientation = -tempDeviceOrientation
        }
        // Calculate desired JPEG orientation.
        return (sensorOrientation + tempDeviceOrientation + 360) % 360
    }

    private fun unlockFocus() {
        try {
            // Reset the auto-focus trigger
            previewRequestBuilder?.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
            setFlash(previewRequestBuilder)
            captureSession?.capture(previewRequestBuilder?.build(), captureCallback,
                    cameraThread.backgroundHandler)
            state = STATE_PREVIEW
            captureSession?.setRepeatingRequest(previewRequest, captureCallback,
                    cameraThread.backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    override fun takePicture(picCallback: PictureCallback) {
        onImageAvailableListener.pictureCallback = picCallback
        lockFocus()
    }

    fun setCamera(facing: Int) {
        if (facing != CAMERA_BACK && facing != CAMERA_FRONT) {
            throw IllegalArgumentException("Invalid camera: $facing")
        }
        cameraFacing = facing
    }

    override fun release() {
        stop()
    }

    override fun stop() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
            cameraThread.stopBackgroundThread()
        }
    }

    @Throws(IOException::class)
    override fun start(textureView: AutoFitTextureView, displayOrientation: Int) {
        this.displayOrientation = displayOrientation
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            if (cameraStarted) {
                return
            }
            cameraStarted = true
            cameraThread.startBackgroundThread()
            this@Camera2Source.textureView = textureView
            openCamera(textureView.width, textureView.height)
        }
    }

    override fun start(surfaceHolder: SurfaceHolder) {
        //Not used
    }

    override fun setFlashMode(flashMode: String) {
        if (flashSupported) {
//            this.flashMode = flashMode
        }
    }

    class PictureDoneCallback : ImageReader.OnImageAvailableListener {
        var pictureCallback: PictureCallback? = null
        override fun onImageAvailable(reader: ImageReader) {
            pictureCallback?.onPictureTaken(reader.acquireNextImage())
        }
    }

    private fun chooseOptimalSize(choices: Array<Size>, textureViewWidth: Int, textureViewHeight: Int,
                                  maxWidth: Int, maxHeight: Int, aspectRatio: Size): Size? {
        val bigEnough = ArrayList<Size>()
        val notBigEnough = ArrayList<Size>()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.width <= maxWidth && option.height <= maxHeight &&
//                    option.height == option.width * h / w) {
                    option.height == option.width * h / w) {
                if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                    bigEnough.add(option)
                } else {
                    notBigEnough.add(option)
                }
            }
        }
        return when {
            bigEnough.size > 0 -> bigEnough.minBy { it.height * it.width }
            notBigEnough.size > 0 -> bigEnough.maxBy { it.height * it.width }
            else -> {
                Log.e(TAG, "Couldn't find any suitable preview size")
                choices[0]
            }
        }
    }

    companion object {
        private const val TAG = "Camera2"
        private const val FOCUS_TAG = "FOCUS_TAG"
        private val ORIENTATIONS = SparseIntArray()
        private val INVERSE_ORIENTATIONS = SparseIntArray()
        private const val STATE_PREVIEW = 0
        private const val STATE_WAITING_LOCK = 1
        private const val STATE_WAITING_PRE_CAPTURE = 2
        private const val STATE_WAITING_NON_PRE_CAPTURE = 3
        private const val STATE_PICTURE_TAKEN = 4
        private const val MAX_PREVIEW_WIDTH = 1920
        private const val MAX_PREVIEW_HEIGHT = 1080

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        init {
            INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270)
            INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180)
            INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90)
            INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0)
        }
    }
}