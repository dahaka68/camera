package com.example.dahaka.mycam.ui.viewModel

import android.arch.lifecycle.ViewModel
import android.support.v4.app.FragmentManager
import com.example.dahaka.mycam.util.router.Router

class DetailViewModel(private val router: Router) : ViewModel() {

    fun startDeleteDialog(manager: FragmentManager) {
        router.startDeleteDialog(manager)
    }

    fun startFaceActivity(file: String) {
        router.startFaceScreen(file)
    }
}