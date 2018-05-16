package com.example.dahaka.mycam.util.camera

import android.view.SurfaceHolder
import com.example.dahaka.mycam.ui.view.AutoFitTextureView

interface CameraDataSource {
    fun takePicture(picCallback: PictureCallback)
    fun setFlashMode(flashMode: String)
    fun stop()
    fun release()
    fun start(textureView: AutoFitTextureView, displayOrientation: Int)
    fun start(surfaceHolder: SurfaceHolder)
}