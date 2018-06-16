package com.example.dahaka.mycam.di.module

import com.example.dahaka.mycam.util.router.RouterImpl
import com.example.dahaka.mycam.ui.camera.util.common.GraphicOverlay
import com.example.dahaka.mycam.ui.viewModel.BarcodeViewModel
import com.example.dahaka.mycam.ui.viewModel.CameraViewModel
import com.example.dahaka.mycam.ui.viewModel.DetailViewModel
import com.example.dahaka.mycam.ui.viewModel.GalleryViewModel
import com.example.dahaka.mycam.util.router.Router
import com.example.dahaka.mycam.ui.barcode.util.BarcodeGraphic
import com.example.dahaka.mycam.ui.camera.util.common.CameraLauncher
import com.google.android.gms.vision.face.FaceDetector
import org.koin.android.architecture.ext.viewModel
import org.koin.dsl.module.applicationContext

val mainModule = applicationContext {
    viewModel { CameraViewModel(get()) }
    viewModel { BarcodeViewModel(get()) }
    viewModel { GalleryViewModel(get()) }
    viewModel { DetailViewModel(get()) }
    bean { RouterImpl(get()) as Router }
    bean { CameraLauncher(get()) }
    bean { GraphicOverlay<BarcodeGraphic>(get()) }
    bean { get<FaceDetector>() }
}