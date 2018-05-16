package com.example.dahaka.mycam.util.barcode

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Camera
import com.example.dahaka.mycam.ui.view.GraphicOverlay
import com.example.dahaka.mycam.util.CAMERA_BACK
import com.example.dahaka.mycam.util.LOW_MEMORY
import com.example.dahaka.mycam.util.camera.CameraSource
import com.example.dahaka.mycam.util.camera.CameraSourcePreview
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.IOException

class BarcodeSource(val context: Context, private val listener: BarcodeGraphicTracker.BarcodeUpdateListener) {
    var cameraSource: CameraSource? = null
    lateinit var preview: CameraSourcePreview
    private lateinit var graphicOverlay: GraphicOverlay<BarcodeGraphic>
    private val lowMemoryLiveData = MutableLiveData<String>()

    fun getLowMemoryData(): LiveData<String> {
        return lowMemoryLiveData
    }

    fun createCameraSource(preview: CameraSourcePreview,
                           graphicOverlay: GraphicOverlay<BarcodeGraphic>, useFlash: Boolean) {
        this.preview = preview
        this.graphicOverlay = graphicOverlay
        val barcodeDetector = BarcodeDetector.Builder(context).build()
        val barcodeFactory = BarcodeTrackerFactory(graphicOverlay, listener)
        barcodeDetector.setProcessor(
                MultiProcessor.Builder(barcodeFactory).build())
        if (!barcodeDetector.isOperational) {
            // Check for low storage.
            val lowStorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = context.registerReceiver(null, lowStorageFilter) != null
            if (hasLowStorage) {
                lowMemoryLiveData.value = LOW_MEMORY
            }
        }
        cameraSource = CameraSource(context, barcodeDetector)
        cameraSource?.setCamera(CAMERA_BACK)
        cameraSource?.setFlashMode(
                if (useFlash) {
                    Camera.Parameters.FLASH_MODE_TORCH
                } else {
                    ""
                })
    }

    @Throws(SecurityException::class)
    fun startCameraSource() {
        try {
            preview.startBarcode(cameraSource, graphicOverlay)
        } catch (e: IOException) {
            e.printStackTrace()
            cameraSource?.release()
            cameraSource = null
        }
    }
}