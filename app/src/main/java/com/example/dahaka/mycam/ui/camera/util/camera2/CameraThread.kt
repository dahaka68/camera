package com.example.dahaka.mycam.ui.camera.util.camera2

import android.os.Build
import android.os.Handler
import android.os.HandlerThread

class CameraThread {
    private var backgroundThread: HandlerThread? = null
    var backgroundHandler: Handler? = null

    fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    fun stopBackgroundThread() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            backgroundThread?.quitSafely()
        } else {
            backgroundThread?.quit()
        }
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}