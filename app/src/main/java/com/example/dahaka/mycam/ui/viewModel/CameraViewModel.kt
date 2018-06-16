package com.example.dahaka.mycam.ui.viewModel

import android.app.Activity
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.example.dahaka.mycam.util.router.Router
import com.example.dahaka.mycam.util.*

class CameraViewModel(private val router: Router) : ViewModel() {
    private var cameraId: String = CAMERA1
    private var flashId: String = FLASH_MODE_AUTO
    private val cameraLiveData = MutableLiveData<String>()
    private val flashLiveData = MutableLiveData<String>()
    private val rotateLiveData = MutableLiveData<Int>()
    private val takePictureLiveData = MutableLiveData<Boolean>()

    fun getCameraId(): LiveData<String> {
        return cameraLiveData
    }

    fun getFlashId(): LiveData<String> {
        return flashLiveData
    }

    fun rotateButtons(value: Int) {
        rotateLiveData.postValue(value)
    }

    fun getButtonsOrientations(): LiveData<Int> {
        return rotateLiveData
    }

    fun changeCamera() {
        if (cameraId == CAMERA1) {
            cameraId = CAMERA2
            cameraLiveData.value = CAMERA2
        } else {
            cameraId = CAMERA1
            cameraLiveData.value = CAMERA1
        }
    }

    fun changeFlashMode() {
        when (flashId) {
            FLASH_MODE_OFF -> {
                flashId = FLASH_MODE_AUTO
                flashLiveData.value = FLASH_MODE_AUTO
            }
            FLASH_MODE_AUTO -> {
                flashId = FLASH_MODE_ON
                flashLiveData.value = FLASH_MODE_ON
            }
            else -> {
                flashId = FLASH_MODE_OFF
                flashLiveData.value = FLASH_MODE_OFF
            }
        }
    }

    fun takePicture() {
        takePictureLiveData.value = true
    }

    fun onPictureTaken(): LiveData<Boolean> {
        return takePictureLiveData
    }

    fun startGallery(activity: Activity) {
        router.openGalleryScreen(activity)
    }

    fun startDetailScreen(activity: Activity, file: String) {
        router.openDetailScreen(activity, file)
    }
}