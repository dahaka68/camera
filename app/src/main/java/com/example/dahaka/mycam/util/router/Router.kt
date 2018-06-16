package com.example.dahaka.mycam.util.router

import android.app.Activity
import android.support.v4.app.FragmentManager

interface Router {
    fun openCameraScreen(activity: Activity)

    fun openBarcodeScreen(activity: Activity)

    fun openGalleryScreen(activity: Activity)

    fun openDetailScreen(activity: Activity, position: Int)

    fun openDetailScreen(activity: Activity, file: String)

    fun openBrowserScreen(activity: Activity, url: String)

    fun startDeleteDialog(manager: FragmentManager)

    fun startFaceScreen(file: String)
}