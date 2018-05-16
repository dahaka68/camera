package com.example.dahaka.mycam.util.camera

import android.graphics.Bitmap
import android.media.Image

interface PictureCallback {
    fun onPictureTaken(image: Image)
    fun onPictureTaken(pic: Bitmap)
}