package com.example.dahaka.mycam.ui.activity

import android.arch.lifecycle.Observer
import android.graphics.Point
import android.hardware.SensorManager
import android.media.MediaActionSound
import android.os.Build
import android.os.Bundle
import android.support.transition.ChangeTransform
import android.support.transition.TransitionManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.OrientationEventListener
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.dahaka.mycam.R
import com.example.dahaka.mycam.ui.view.GraphicOverlay
import com.example.dahaka.mycam.ui.viewModel.CameraViewModel
import com.example.dahaka.mycam.util.CAMERA1
import com.example.dahaka.mycam.util.FLASH_MODE_AUTO
import com.example.dahaka.mycam.util.FLASH_MODE_ON
import com.example.dahaka.mycam.util.camera.CameraLauncher
import com.example.dahaka.mycam.util.camera.CameraSourcePreview
import kotlinx.android.synthetic.main.activity_camera.*
import org.koin.android.architecture.ext.viewModel
import org.koin.android.ext.android.inject

class CameraActivity : AppCompatActivity(), View.OnClickListener {
    private val cameraViewModel by viewModel<CameraViewModel>()
    private val cameraLauncher: CameraLauncher by inject()
    private val preview: CameraSourcePreview by lazy {
        findViewById<CameraSourcePreview>(R.id.preview)
    }
    private val graphicOverlay: GraphicOverlay<GraphicOverlay.Graphic> by lazy {
        findViewById<GraphicOverlay<GraphicOverlay.Graphic>>(R.id.face_overlay)
    }
    private var path: String = ""
    private var wasActivityResumed = false
    private var useCamera2api = false
    private var usingBackCamera = true
    private val sound = MediaActionSound()
    private val changeTransform = ChangeTransform().apply {
        duration = 200
        interpolator = AccelerateInterpolator()
    }

    private val listener by lazy {
        object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
                cameraViewModel.rotateButtons(orientation)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (checkSoftwareButtons()) {
            setContentView(R.layout.activity_camera_software)
        } else {
            setContentView(R.layout.activity_camera)
        }
        cameraViewModel.getCameraId().observe(this, Observer { camera ->
            usingBackCamera = if (camera == CAMERA1) {
                flashContainer.visibility = View.VISIBLE
                cameraFlash.visibility = View.VISIBLE
                cameraLauncher.stopCameraSource()
                cameraLauncher.createCameraSourceBack(preview, graphicOverlay)
                true
            } else {
                flashContainer.visibility = View.GONE
                cameraFlash.visibility = View.GONE
                cameraLauncher.stopCameraSource()
                cameraLauncher.createCameraSourceFront(preview, graphicOverlay)
                false
            }
        })
        cameraViewModel.getFlashId().observe(this, Observer { flash ->
            if (usingBackCamera) {
                cameraLauncher.setFlash(flash)
            }
            when (flash) {
                FLASH_MODE_ON -> cameraFlash.setImageResource(R.drawable.ic_camera_flash)
                FLASH_MODE_AUTO -> cameraFlash.setImageResource(R.drawable.ic_automatic_flash)
                else -> cameraFlash.setImageResource(R.drawable.ic_flash_off)
            }
        })
        cameraViewModel.getButtonsOrientations().observe(this, Observer { value ->
            if (useCamera2api) {
                TransitionManager.beginDelayedTransition(control, changeTransform)
            }
            value?.let { rotateInterface(value) }
        })
        cameraViewModel.onPictureTaken().observe(this, Observer { _ ->
            takePicture()
        })
        cameraLauncher.getFilePath().observe(this, Observer { path ->
            path?.let {
                this.path = path
                setPreview(path)
            }
        })
        findViewById<ImageView>(R.id.picture).setOnClickListener(this)
        findViewById<View>(R.id.rotateContainer).setOnClickListener(this)
        findViewById<View>(R.id.flashContainer).setOnClickListener(this)
        findViewById<ImageView>(R.id.gallery).setOnClickListener(this)
        findViewById<ImageView>(R.id.imagePreview).setOnClickListener(this)
        useCamera2api = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        cameraLauncher.useCamera2api(useCamera2api)
        cameraLauncher.createCameraSourceBack(preview, graphicOverlay)
    }

    private fun checkSoftwareButtons(): Boolean {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        return size.y < size.x / 9 * 16
    }

    private fun rotateInterface(value: Int) {
        when (value) {
            in 45..134 -> {
                cameraLauncher.rotatePicture(180f)
                rotateIcons(270f)
            }
            in 135..224 -> {
                if (usingBackCamera) {
                    cameraLauncher.rotatePicture(270f)
                } else {
                    cameraLauncher.rotatePicture(90f)
                }
                rotateIcons(180f)
            }
            in 225..314 -> {
                cameraLauncher.rotatePicture(360f)
                rotateIcons(90f)
            }
            in 315..360 -> {
                if (usingBackCamera) {
                    cameraLauncher.rotatePicture(90f)
                } else {
                    cameraLauncher.rotatePicture(270f)
                }
                rotateIcons(0f)
            }
            in 0..44 -> {
                if (usingBackCamera) {
                    cameraLauncher.rotatePicture(90f)
                } else {
                    cameraLauncher.rotatePicture(270f)
                }
                rotateIcons(0f)
            }
        }
    }

    private fun rotateIcons(deg: Float) {
        galleryIcon.rotation = deg
        rotateCamera.rotation = deg
        imagePreview.rotation = deg
        cameraFlash.rotation = deg
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.picture -> {
                cameraViewModel.takePicture()
            }
            R.id.rotateContainer -> cameraViewModel.changeCamera()
            R.id.flashContainer -> cameraViewModel.changeFlashMode()
            R.id.gallery -> cameraViewModel.startGallery(this)
            R.id.imagePreview -> {
                if (path.isNotEmpty()) {
                    cameraViewModel.startDetailScreen(this, path)
                }
            }
        }
    }

    private fun takePicture() {
        cameraLauncher.takePicture()
        playCaptureSound()
    }

    override fun onResume() {
        super.onResume()
        if (listener.canDetectOrientation()) {
            Log.v(TAG, "Can detect orientation")
            listener.enable()
        } else {
            Log.v(TAG, "Cannot detect orientation")
            listener.disable()
        }
        if (wasActivityResumed) {
            cameraLauncher.wasResumed(usingBackCamera)
        }
    }

    override fun onPause() {
        super.onPause()
        wasActivityResumed = true
        cameraLauncher.stopCameraSource()
    }

    override fun onStop() {
        super.onStop()
        listener.disable()
    }

    private fun playCaptureSound() {
        sound.play(MediaActionSound.SHUTTER_CLICK)
    }

    private fun setPreview(file: String) {
        Glide.with(this)
                .load(file)
                .apply(RequestOptions().circleCrop())
                .into(imagePreview)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraLauncher.stopCameraSource()
        if (!useCamera2api) {
            cameraLauncher.previewFaceDetector.release()
        }
    }

    companion object {
        private const val TAG = "CameraActivity"
    }
}