package com.example.christmastrees

import android.app.Application
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize osmdroid configuration directly without load()
        // to avoid lifecycle observer registration issues
        val config = Configuration.getInstance()
        config.setUserAgentValue("ChristmasTrees/1.0")
        config.tileFileSystemCacheMaxBytes = 50L * 1024 * 1024 * 1024
    }
}
