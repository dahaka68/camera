package com.example.dahaka.mycam.util.camera

import android.content.Context
import com.example.dahaka.mycam.ui.view.FaceGraphic
import com.example.dahaka.mycam.ui.view.GraphicOverlay
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face

class GraphicFaceTrackerFactory(val context: Context, val graphicOverlay: GraphicOverlay<GraphicOverlay.Graphic>)
    : MultiProcessor.Factory<Face> {
    private lateinit var faceGraphic: FaceGraphic
    override fun create(face: Face): Tracker<Face> {
        return GraphicFaceTracker(graphicOverlay)
    }

    private inner class GraphicFaceTracker(val overlay: GraphicOverlay<GraphicOverlay.Graphic>) : Tracker<Face>() {
        init {
            faceGraphic = FaceGraphic(overlay, context)
        }

        override fun onNewItem(faceId: Int, item: Face?) {
            faceGraphic.setId(faceId)
        }

        override fun onUpdate(detectionResults: Detector.Detections<Face>?, face: Face?) {
            overlay.add(faceGraphic)
            faceGraphic.updateFace(face)
        }

        override fun onMissing(detectionResults: Detector.Detections<Face>?) {
            faceGraphic.goneFace()
            overlay.remove(faceGraphic)
        }

        override fun onDone() {
            faceGraphic.goneFace()
            overlay.remove(faceGraphic)
        }
    }
}