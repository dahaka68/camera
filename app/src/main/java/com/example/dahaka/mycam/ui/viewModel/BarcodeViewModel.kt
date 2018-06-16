package com.example.dahaka.mycam.ui.viewModel

import android.app.Activity
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.example.dahaka.mycam.util.FLASH_MODE_OFF
import com.example.dahaka.mycam.util.FLASH_MODE_TORCH
import com.example.dahaka.mycam.util.router.Router

class BarcodeViewModel(private val router: Router) : ViewModel() {
    private var flashId: String = FLASH_MODE_OFF
    private val flashLiveData = MutableLiveData<String>()

    fun getFlashId(): LiveData<String> {
        return flashLiveData
    }

    fun changeFlashMode() {
        when (flashId) {
            FLASH_MODE_OFF -> {
                flashId = FLASH_MODE_TORCH
                flashLiveData.value = FLASH_MODE_TORCH
            }
            else -> {
                flashId = FLASH_MODE_OFF
                flashLiveData.value = FLASH_MODE_OFF
            }
        }
    }

    fun openBrowser(activity: Activity, url: String) {
        router.openBrowserScreen(activity, url)
    }
}