package com.example.dahaka.mycam.ui.camera.util.common

import android.view.SurfaceHolder
import com.example.dahaka.mycam.ui.camera.util.camera2.AutoFitTextureView

interface CameraDataSource {
    fun takePicture(picCallback: PictureCallback)
    fun setFlashMode(flashMode: String)
    fun stop()
    fun release()
    fun start(textureView: AutoFitTextureView, displayOrientation: Int)
    fun start(surfaceHolder: SurfaceHolder)
}