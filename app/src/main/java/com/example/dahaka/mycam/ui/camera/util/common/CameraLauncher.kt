package com.example.dahaka.mycam.ui.camera.util.common

import android.annotation.TargetApi
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.os.Build
import android.os.Environment
import android.util.Log
import com.example.dahaka.mycam.ui.camera.util.camera1.CameraSource
import com.example.dahaka.mycam.ui.camera.util.camera2.Camera2Source
import com.example.dahaka.mycam.util.APP_NAME
import com.example.dahaka.mycam.util.CAMERA_BACK
import com.example.dahaka.mycam.util.DATE_FORMAT
import com.google.android.gms.vision.face.FaceDetector
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CameraLauncher(val context: Context) {
    private var sensorOrientation = 0f
    private var cameraSource: CameraDataSource? = null
    private var useCamera2api = false
    private lateinit var preview: CameraSourcePreview
    private lateinit var graphicOverlay: GraphicOverlay<GraphicOverlay.Graphic>
    private val filePathLiveData = MutableLiveData<String>()
    lateinit var previewFaceDetector: FaceDetector
    lateinit var file: File

    fun getFilePath(): LiveData<String> {
        return filePathLiveData
    }

    private val cameraSourcePictureCallback = object : PictureCallback {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onPictureTaken(image: Image) {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)
            val picture = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
            val rotatedPic = getRotatedPic(picture, sensorOrientation)
            var out: FileOutputStream? = null
            try {
//                file = getPathForImage()
//                file = context.externalCacheDir
                file = File(context.externalCacheDir, "image.jpg")
                out = FileOutputStream(file)
                rotatedPic.compress(Bitmap.CompressFormat.JPEG, 90, out)
                filePathLiveData.postValue(file.path)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                image.close()
                out?.let {
                    try {
                        it.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        override fun onPictureTaken(pic: Bitmap) {
            val rotatedPic = getRotatedPic(pic, sensorOrientation)
            var out: FileOutputStream? = null
            try {
                file = getPathForImage()
                out = FileOutputStream(file)
                rotatedPic.compress(Bitmap.CompressFormat.JPEG, 90, out)
                filePathLiveData.postValue(file.path)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                out?.let {
                    try {
                        it.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun useCamera2api(isCamera2: Boolean) {
        useCamera2api = isCamera2
    }

    fun takePicture() {
        cameraSource?.takePicture(cameraSourcePictureCallback)
    }

    private fun getPathForImage(): File {
        val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), APP_NAME)
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory")
            }
        }
        val timeStamp = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
                .format(Date())
        return File("${mediaStorageDir.path}${File.separator}IMG_$timeStamp.jpg")
    }

    private fun getRotatedPic(picture: Bitmap, deg: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(deg)
        return Bitmap.createBitmap(picture, 0, 0, picture.width, picture.height,
                matrix, true)
    }

    fun createCameraSourceBack(preview: CameraSourcePreview, graphicOverlay: GraphicOverlay<GraphicOverlay.Graphic>) {
        this.preview = preview
        this.graphicOverlay = graphicOverlay
        if (useCamera2api) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cameraSource = Camera2Source(context)
                (cameraSource as Camera2Source).setCamera(CAMERA_BACK)
            }
            startCameraSource(preview, graphicOverlay)
        } else {
            cameraSource = CameraSource(context)
            (cameraSource as CameraSource).setCamera(CAMERA_BACK)
            startCameraSource(preview, graphicOverlay)
        }
    }

    private fun startCameraSource(preview: CameraSourcePreview, graphicOverlay: GraphicOverlay<GraphicOverlay.Graphic>) {
        try {
            preview.start(cameraSource, graphicOverlay)
        } catch (e: IOException) {
            cameraSource?.release()
            cameraSource = null
        }
    }

    fun stopCameraSource() {
        preview.stop()
    }

    fun wasResumed() {
        if (useCamera2api) {
            createCameraSourceBack(preview, graphicOverlay)
        } else {
            startCameraSource(preview, graphicOverlay)
        }
    }

    fun setFlash(flash: String?) {
        cameraSource?.setFlashMode(flash ?: "0")
    }

    fun rotatePicture(orientation: Float) {
        sensorOrientation = orientation
    }

    companion object {
        private const val TAG = "CameraLauncher"
    }
}