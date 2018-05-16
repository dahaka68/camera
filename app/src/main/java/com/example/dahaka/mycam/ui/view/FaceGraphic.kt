package com.example.dahaka.mycam.ui.view

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import com.example.dahaka.mycam.R
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.Landmark

class FaceGraphic(overlay: GraphicOverlay<*>, context: Context) : GraphicOverlay.Graphic(overlay) {
    private val marker: Bitmap
    private val opt: BitmapFactory.Options = BitmapFactory.Options()
    private val resources: Resources
    private var faceId: Int = 0
    private lateinit var facePosition: PointF
    private var faceWidth: Float = 0.toFloat()
    private var faceHeight: Float = 0.toFloat()
    private lateinit var faceCenter: PointF
    private var smilingProbability = -1f
    private var eyeRightOpenProbability = -1f
    private var eyeLeftOpenProbability = -1f
    private var eulerZ: Float = 0.toFloat()
    private var eulerY: Float = 0.toFloat()
    private lateinit var leftEyePos: PointF
    private lateinit var rightEyePos: PointF
    private lateinit var noseBasePos: PointF
    private lateinit var leftMouthCorner: PointF
    private lateinit var rightMouthCorner: PointF
    private lateinit var mouthBase: PointF
    private lateinit var leftEar: PointF
    private lateinit var rightEar: PointF
    private lateinit var leftEarTip: PointF
    private lateinit var rightEarTip: PointF
    private lateinit var leftCheek: PointF
    private lateinit var rightCheek: PointF
    private var face: Face? = null

    init {
        opt.inScaled = false
        resources = context.resources
        marker = BitmapFactory.decodeResource(resources, R.drawable.marker, opt)
    }

    fun setId(id: Int) {
        faceId = id
    }

    fun updateFace(face: Face?) {
        this.face = face
        postInvalidate()
    }

    fun goneFace() {
        face = null
    }

    override fun draw(canvas: Canvas) {
        val face = face
        if (face == null) {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR)
            smilingProbability = -1f
            eyeRightOpenProbability = -1f
            eyeLeftOpenProbability = -1f
            return
        }
        facePosition = PointF(translateX(face.position.x), translateY(face.position.y))
        faceWidth = face.width * 4
        faceHeight = face.height * 4
        faceCenter = PointF(translateX(face.position.x + faceWidth / 8), translateY(face.position.y + faceHeight / 8))
        smilingProbability = face.isSmilingProbability
        eyeRightOpenProbability = face.isRightEyeOpenProbability
        eyeLeftOpenProbability = face.isLeftEyeOpenProbability
        eulerY = face.eulerY
        eulerZ = face.eulerZ
        for (landmark in face.landmarks) {
            when (landmark.type) {
                Landmark.LEFT_EYE -> leftEyePos = PointF(translateX(landmark.position.x), translateY(landmark.position.y))
                Landmark.RIGHT_EYE -> rightEyePos = PointF(translateX(landmark.position.x), translateY(landmark.position.y))
                Landmark.NOSE_BASE -> noseBasePos = PointF(translateX(landmark.position.x), translateY(landmark.position.y))
                Landmark.LEFT_MOUTH -> leftMouthCorner = PointF(translateX(landmark.position.x), translateY(landmark.position.y))
                Landmark.RIGHT_MOUTH -> rightMouthCorner = PointF(translateX(landmark.position.x), translateY(landmark.position.y))
                Landmark.BOTTOM_MOUTH -> mouthBase = PointF(translateX(landmark.position.x), translateY(landmark.position.y))
                Landmark.LEFT_EAR -> leftEar = PointF(translateX(landmark.position.x), translateY(landmark.position.y))
                Landmark.RIGHT_EAR -> rightEar = PointF(translateX(landmark.position.x), translateY(landmark.position.y))
                Landmark.LEFT_EAR_TIP -> leftEarTip = PointF(translateX(landmark.position.x), translateY(landmark.position.y))
                Landmark.RIGHT_EAR_TIP -> rightEarTip = PointF(translateX(landmark.position.x), translateY(landmark.position.y))
                Landmark.LEFT_CHEEK -> leftCheek = PointF(translateX(landmark.position.x), translateY(landmark.position.y))
                Landmark.RIGHT_CHEEK -> rightCheek = PointF(translateX(landmark.position.x), translateY(landmark.position.y))
            }
        }
        val mPaint = Paint()
        mPaint.color = Color.WHITE
        mPaint.strokeWidth = 4f
        canvas.drawBitmap(marker, faceCenter.x, faceCenter.y, null)
        if (this::noseBasePos.isInitialized)
            canvas.drawBitmap(marker, noseBasePos.x, noseBasePos.y, null)
        if (this::leftEyePos.isInitialized)
            canvas.drawBitmap(marker, leftEyePos.x, leftEyePos.y, null)
        if (this::rightEyePos.isInitialized)
            canvas.drawBitmap(marker, rightEyePos.x, rightEyePos.y, null)
        if (this::mouthBase.isInitialized)
            canvas.drawBitmap(marker, mouthBase.x, mouthBase.y, null)
        if (this::leftMouthCorner.isInitialized)
            canvas.drawBitmap(marker, leftMouthCorner.x, leftMouthCorner.y, null)
        if (this::rightMouthCorner.isInitialized)
            canvas.drawBitmap(marker, rightMouthCorner.x, rightMouthCorner.y, null)
        if (this::leftEar.isInitialized)
            canvas.drawBitmap(marker, leftEar.x, leftEar.y, null)
        if (this::rightEar.isInitialized)
            canvas.drawBitmap(marker, rightEar.x, rightEar.y, null)
        if (this::leftEarTip.isInitialized)
            canvas.drawBitmap(marker, leftEarTip.x, leftEarTip.y, null)
        if (this::rightEarTip.isInitialized)
            canvas.drawBitmap(marker, rightEarTip.x, rightEarTip.y, null)
        if (this::leftCheek.isInitialized)
            canvas.drawBitmap(marker, leftCheek.x, leftCheek.y, null)
        if (this::rightCheek.isInitialized)
            canvas.drawBitmap(marker, rightCheek.x, rightCheek.y, null)
    }
}