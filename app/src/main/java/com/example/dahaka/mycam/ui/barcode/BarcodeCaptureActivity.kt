package com.example.dahaka.mycam.ui.barcode

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.WindowManager
import android.widget.Toast
import com.example.dahaka.mycam.R
import com.example.dahaka.mycam.ui.camera.util.common.GraphicOverlay
import com.example.dahaka.mycam.ui.viewModel.BarcodeViewModel
import com.example.dahaka.mycam.util.FLASH_MODE_OFF
import com.example.dahaka.mycam.util.FLASH_MODE_TORCH
import com.example.dahaka.mycam.util.LOW_MEMORY
import com.example.dahaka.mycam.ui.barcode.util.BarcodeGraphic
import com.example.dahaka.mycam.ui.barcode.util.BarcodeGraphicTracker
import com.example.dahaka.mycam.ui.barcode.util.BarcodeSource
import com.example.dahaka.mycam.ui.camera.util.common.CameraSourcePreview
import com.google.android.gms.vision.barcode.Barcode
import kotlinx.android.synthetic.main.barcode_capture.*
import org.koin.android.architecture.ext.viewModel

class BarcodeCaptureActivity : AppCompatActivity(), BarcodeGraphicTracker.BarcodeUpdateListener {
    private val barcodeViewModel by viewModel<BarcodeViewModel>()
    private val barcodeSource by lazy { BarcodeSource(this, this) }
    private val preview: CameraSourcePreview by lazy {
        findViewById<CameraSourcePreview>(R.id.preview)
    }
    private val graphicOverlay: GraphicOverlay<BarcodeGraphic> by lazy {
        findViewById<GraphicOverlay<BarcodeGraphic>>(R.id.graphicOverlay)
    }
    private val scaleGestureDetector: ScaleGestureDetector by lazy {
        ScaleGestureDetector(this, ScaleListener())
    }

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.barcode_capture)
        barcodeSource.createCameraSource(preview, graphicOverlay, false)
        barcodeViewModel.getFlashId().observe(this, Observer { flash ->
            barcodeSource.cameraSource?.setFlash(flash)
            if (flash == FLASH_MODE_TORCH) {
                imageFlash.setImageResource(R.drawable.ic_camera_flash)
                barcodeSource.cameraSource?.setFlashMode(FLASH_MODE_TORCH)
            } else {
                imageFlash.setImageResource(R.drawable.ic_flash_off)
                barcodeSource.cameraSource?.setFlashMode(FLASH_MODE_OFF)
            }
        })
        barcodeSource.getLowMemoryData().observe(this, Observer { data ->
            if (data == LOW_MEMORY) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show()
            }
        })
        Snackbar.make(graphicOverlay, getString(R.string.zoom_message),
                Snackbar.LENGTH_LONG)
                .show()
        barcodeFlash.setOnClickListener {
            barcodeViewModel.changeFlashMode()
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val b = scaleGestureDetector.onTouchEvent(e)
        return b || super.onTouchEvent(e)
    }

    override fun onResume() {
        super.onResume()
        barcodeSource.startCameraSource()
    }

    override fun onPause() {
        super.onPause()
        barcodeSource.preview.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        barcodeSource.preview.release()
    }

    private inner class ScaleListener : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            return false
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            barcodeSource.cameraSource?.doZoom(detector.scaleFactor)
        }
    }

    override fun onBarcodeDetected(barcode: Barcode?) {
        if (barcode != null) {
            Snackbar.make(graphicOverlay, barcode.displayValue,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.more, { barcodeViewModel.openBrowser(this, barcode.displayValue) })
                    .setActionTextColor(ActivityCompat.getColor(this, R.color.blue))
                    .show()
        }
    }
}