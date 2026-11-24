package com.lightshare

import android.app.Application
import com.lightshare.di.AppContainer

class LightShareApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.receiveStorage.purge() // remove temp files left by a killed process
    }
}
