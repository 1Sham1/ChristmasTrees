package com.example.christmastrees.util

import android.content.Context
import android.content.Context.MODE_PRIVATE

object SettingsManager {

    private const val PREFS_NAME = "app_settings"
    private const val KEY_MUSIC_ENABLED = "music_enabled"
    private const val KEY_SNOW_ENABLED = "snow_enabled"

    private const val MAP_PREFS_NAME = "map_settings"
    private const val KEY_MAP_LAT = "lat"
    private const val KEY_MAP_LON = "lon"
    private const val KEY_MAP_ZOOM = "zoom"

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (!prefs.contains(KEY_MUSIC_ENABLED)) {
            prefs.edit().putBoolean(KEY_MUSIC_ENABLED, true).apply()
        }
        if (!prefs.contains(KEY_SNOW_ENABLED)) {
            prefs.edit().putBoolean(KEY_SNOW_ENABLED, true).apply()
        }
    }

    fun isMusicEnabled(context: Context, profileName: String): Boolean {
        val prefs = context.getSharedPreferences("app_settings_$profileName", MODE_PRIVATE)
        return prefs.getBoolean(KEY_MUSIC_ENABLED, true)
    }

    fun saveMusicEnabled(context: Context, enabled: Boolean, profileName: String) {
        val prefs = context.getSharedPreferences("app_settings_$profileName", MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply()
    }

    fun isSnowEnabled(context: Context, profileName: String): Boolean {
        val prefs = context.getSharedPreferences("app_settings_$profileName", MODE_PRIVATE)
        return prefs.getBoolean(KEY_SNOW_ENABLED, true)
    }

    fun saveSnowEnabled(context: Context, enabled: Boolean, profileName: String) {
        val prefs = context.getSharedPreferences("app_settings_$profileName", MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SNOW_ENABLED, enabled).apply()
    }

    fun saveMapPosition(context: Context, lat: Float, lon: Float, zoom: Float, profileName: String) {
        val prefs = context.getSharedPreferences("map_settings_$profileName", MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_MAP_LAT, lat)
            .putFloat(KEY_MAP_LON, lon)
            .putFloat(KEY_MAP_ZOOM, zoom)
            .apply()
    }

    fun loadMapPosition(context: Context, profileName: String): MapPosition? {
        val prefs = context.getSharedPreferences("map_settings_$profileName", MODE_PRIVATE)
        val lat = prefs.getFloat(KEY_MAP_LAT, 0f)
        val lon = prefs.getFloat(KEY_MAP_LON, 0f)
        val zoom = prefs.getFloat(KEY_MAP_ZOOM, 0f)
        if (lat == 0f && lon == 0f && zoom == 0f) return null
        return MapPosition(lat, lon, zoom)
    }

    data class MapPosition(
        val lat: Float,
        val lon: Float,
        val zoom: Float
    )
}
