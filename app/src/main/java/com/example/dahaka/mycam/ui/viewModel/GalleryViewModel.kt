package com.example.dahaka.mycam.ui.viewModel

import android.app.Activity
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Environment
import android.support.v4.app.FragmentManager
import com.example.dahaka.mycam.Router
import com.example.dahaka.mycam.util.APP_NAME
import java.io.File

class GalleryViewModel(private val router: Router) : ViewModel() {
    private var spanCount = 3
    private val spanCountLiveData = MutableLiveData<Int>()
    private val filesInStorageLiveData = MutableLiveData<Int>()
    private var rvPosition = 0

    fun getSpanCount(): LiveData<Int> {
        return spanCountLiveData
    }

    fun setRvPosition(position: Int) {
        rvPosition = position
    }

    fun getRvPosition(): Int {
        return rvPosition
    }

    fun getFilesQuantity(): LiveData<Int> {
        return filesInStorageLiveData
    }

    fun refreshGalleryItemsCount() {
        val f = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), APP_NAME)
        if (f.listFiles() != null) {
            filesInStorageLiveData.value = f.listFiles().size
        }
    }

    fun changeSpanCount() {
        when (spanCount) {
            2 -> {
                spanCount = 1
                spanCountLiveData.value = 1
            }
            3 -> {
                spanCount = 2
                spanCountLiveData.value = 2
            }
            else -> {
                spanCount = 3
                spanCountLiveData.value = 3
            }
        }
    }

    fun startDetailActivity(activity: Activity, position: Int) {
        router.openDetailScreen(activity, position)
    }

    fun startCameraActivity(activity: Activity) {
        router.openCameraScreen(activity)
    }

    fun startBarcodeActivity(activity: Activity) {
        router.openBarcodeScreen(activity)
    }

    fun startDeleteDialog(manager: FragmentManager) {
        router.startDeleteDialog(manager)
    }
}