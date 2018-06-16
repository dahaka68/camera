package com.example.dahaka.mycam.ui.barcode.util

import com.example.dahaka.mycam.ui.camera.util.common.GraphicOverlay

import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.barcode.Barcode

class BarcodeTrackerFactory(private val graphicOverlay: GraphicOverlay<BarcodeGraphic>,
                            private val listener: BarcodeGraphicTracker.BarcodeUpdateListener)
    : MultiProcessor.Factory<Barcode> {

    override fun create(barcode: Barcode): Tracker<Barcode>
            = BarcodeGraphicTracker(graphicOverlay, BarcodeGraphic(graphicOverlay), listener)
}