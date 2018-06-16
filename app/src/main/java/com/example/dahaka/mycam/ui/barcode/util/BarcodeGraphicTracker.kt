package com.example.dahaka.mycam.ui.barcode.util

import android.support.annotation.UiThread
import com.example.dahaka.mycam.ui.camera.util.common.GraphicOverlay
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.barcode.Barcode

class BarcodeGraphicTracker(private val overlay: GraphicOverlay<BarcodeGraphic>,
                            private val graphic: BarcodeGraphic,
                            context: BarcodeUpdateListener) : Tracker<Barcode>() {

    private var barcodeUpdateListener: BarcodeUpdateListener = context

    interface BarcodeUpdateListener {
        @UiThread
        fun onBarcodeDetected(barcode: Barcode?)
    }

    override fun onNewItem(id: Int, item: Barcode?) {
        barcodeUpdateListener.onBarcodeDetected(item)
    }

    override fun onUpdate(detectionResults: Detector.Detections<Barcode>, item: Barcode) {
        overlay.add(graphic)
        graphic.updateItem(item)
    }

    override fun onMissing(detectionResults: Detector.Detections<Barcode>) {
        overlay.remove(graphic)
    }

    override fun onDone() {
        overlay.remove(graphic)
    }
}