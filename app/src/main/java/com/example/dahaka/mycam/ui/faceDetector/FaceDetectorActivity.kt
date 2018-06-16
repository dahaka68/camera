package com.example.dahaka.mycam.ui.faceDetector

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.dahaka.mycam.R
import com.example.dahaka.mycam.ui.detail.DetailActivity
import com.example.dahaka.mycam.ui.faceDetector.util.FaceDetector
import kotlinx.android.synthetic.main.activity_face_detector.*

class FaceDetectorActivity : AppCompatActivity() {
    private val filePath by lazy { intent.getStringExtra(DetailActivity.FACE_FILE_PATH) }
    private val faceDetector by lazy { FaceDetector(this, filePath) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detector)
    }

    override fun onResume() {
        super.onResume()
        Glide.with(this)
                .load(faceDetector.getFaceImage())
                .into(faceImage)
    }
}