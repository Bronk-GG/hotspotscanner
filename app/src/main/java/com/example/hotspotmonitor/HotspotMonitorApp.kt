package com.example.hotspotmonitor

import android.app.Application
import com.example.hotspotmonitor.data.AppRepository

class HotspotMonitorApp : Application() {
    lateinit var repository: AppRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(this)
    }
}
