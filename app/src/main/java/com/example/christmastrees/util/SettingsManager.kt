package com.example.christmastrees.util

import android.content.Context
import android.content.Context.MODE_PRIVATE

object SettingsManager {

    private var _initialized = false
    private val initLock = Any()

    private const val PREFS_NAME = "app_settings"
    private const val KEY_MUSIC_ENABLED = "music_enabled"
    private const val KEY_SNOW_ENABLED = "snow_enabled"

    fun init(context: Context) {
        synchronized(initLock) {
            if (_initialized) return
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            if (!prefs.contains(KEY_MUSIC_ENABLED)) {
                prefs.edit().putBoolean(KEY_MUSIC_ENABLED, true).apply()
            }
            if (!prefs.contains(KEY_SNOW_ENABLED)) {
                prefs.edit().putBoolean(KEY_SNOW_ENABLED, true).apply()
            }
            _initialized = true
        }
    }

    fun isMusicEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getBoolean(KEY_MUSIC_ENABLED, true)
    }

    fun saveMusicEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply()
    }

    fun isSnowEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getBoolean(KEY_SNOW_ENABLED, true)
    }

    fun saveSnowEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SNOW_ENABLED, enabled).apply()
    }
}
