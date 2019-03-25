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
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val mainModule = module {
    viewModel { CameraViewModel(get()) }
    viewModel { BarcodeViewModel(get()) }
    viewModel { GalleryViewModel(get()) }
    viewModel { DetailViewModel(get()) }
    single { RouterImpl(get()) as Router }
    single { CameraLauncher(get()) }
    single { GraphicOverlay<BarcodeGraphic>(get()) }
    single { get<FaceDetector>() }
}