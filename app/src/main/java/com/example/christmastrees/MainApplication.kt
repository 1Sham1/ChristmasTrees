package com.example.christmastrees

import android.app.Application
import com.example.christmastrees.util.AudioManager
import com.example.christmastrees.util.SettingsManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize SettingsManager for persisting app preferences
        SettingsManager.init(this)

        // Initialize osmdroid configuration directly without load()
        // to avoid lifecycle observer registration issues
        val config = Configuration.getInstance()
        config.setUserAgentValue("ChristmasTrees/1.0")
        config.tileFileSystemCacheMaxBytes = 50L * 1024 * 1024 * 1024

        // Initialize AudioManager for background music
        AudioManager.init(this)
    }
}
