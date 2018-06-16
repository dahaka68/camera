package com.example.dahaka.mycam.ui.camera.util.common

import android.graphics.Bitmap
import android.media.Image

interface PictureCallback {
    fun onPictureTaken(image: Image)
    fun onPictureTaken(pic: Bitmap)
}