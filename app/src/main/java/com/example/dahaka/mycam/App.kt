package com.example.dahaka.mycam

import android.app.Application
import com.example.dahaka.mycam.di.module.apiModule
import com.example.dahaka.mycam.di.module.mainModule
import org.koin.android.ext.android.startKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin(this, listOf(mainModule, apiModule))
    }
}