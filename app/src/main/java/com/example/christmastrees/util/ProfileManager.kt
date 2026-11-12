package com.example.christmastrees.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.christmastrees.data.ExportProfile
import com.example.christmastrees.data.EXPORT_VERSION
import com.example.christmastrees.viewmodel.Marker
import android.util.Log

object ProfileManager {

    private const val PROFILES_PREFS = "profiles_data"
    private const val KEY_PROFILES = "profiles"
    private const val KEY_ACTIVE_PROFILE = "active_profile"

    private const val DEFAULT_PROFILE_NAME = "Default"

    private val gson = Gson()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PROFILES_PREFS, MODE_PRIVATE)

        // Check if profiles have been initialized
        val profilesJson = prefs.getString(KEY_PROFILES, null)
        if (profilesJson == null) {
            // First run - create Default profile
            val profiles = listOf(DEFAULT_PROFILE_NAME)
            prefs.edit()
                .putString(KEY_PROFILES, gson.toJson(profiles))
                .putString(KEY_ACTIVE_PROFILE, DEFAULT_PROFILE_NAME)
                .apply()

            // Migrate existing data to Default profile if present
            migrateDataToProfile(context, DEFAULT_PROFILE_NAME)
        }
    }

    fun getProfileNames(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PROFILES_PREFS, MODE_PRIVATE)
        val profilesJson = prefs.getString(KEY_PROFILES, "[]")
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(profilesJson, type) ?: listOf(DEFAULT_PROFILE_NAME)
    }

    fun getActiveProfile(context: Context): String {
        val prefs = context.getSharedPreferences(PROFILES_PREFS, MODE_PRIVATE)
        return prefs.getString(KEY_ACTIVE_PROFILE, DEFAULT_PROFILE_NAME) ?: DEFAULT_PROFILE_NAME
    }

    fun setActiveProfile(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PROFILES_PREFS, MODE_PRIVATE)
        prefs.edit().putString(KEY_ACTIVE_PROFILE, name).commit()
    }

    fun createProfile(context: Context, name: String): Boolean {
        if (name.isBlank() || name == DEFAULT_PROFILE_NAME) return false

        val prefs = context.getSharedPreferences(PROFILES_PREFS, MODE_PRIVATE)
        val profilesJson = prefs.getString(KEY_PROFILES, "[]")
        val type = object : TypeToken<List<String>>() {}.type
        val profiles: MutableList<String> = gson.fromJson(profilesJson, type) ?: mutableListOf()

        if (profiles.contains(name)) return false

        profiles.add(name)
        prefs.edit().putString(KEY_PROFILES, gson.toJson(profiles)).apply()
        return true
    }

    fun deleteProfile(context: Context, name: String): Boolean {
        if (name == DEFAULT_PROFILE_NAME) return false

        val prefs = context.getSharedPreferences(PROFILES_PREFS, MODE_PRIVATE)
        val profilesJson = prefs.getString(KEY_PROFILES, "[]")
        val type = object : TypeToken<List<String>>() {}.type
        val profiles: MutableList<String> = gson.fromJson(profilesJson, type) ?: mutableListOf()

        if (!profiles.contains(name)) return false

        profiles.remove(name)
        prefs.edit().putString(KEY_PROFILES, gson.toJson(profiles)).apply()

        // Delete all SharedPreferences files for this profile
        context.getSharedPreferences("markers_data_$name", MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("app_settings_$name", MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("map_settings_$name", MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("custom_marker_types_$name", MODE_PRIVATE).edit().clear().apply()

        // If the deleted profile was active, switch to Default
        val activeProfile = prefs.getString(KEY_ACTIVE_PROFILE, DEFAULT_PROFILE_NAME)
        if (activeProfile == name) {
            prefs.edit().putString(KEY_ACTIVE_PROFILE, DEFAULT_PROFILE_NAME).apply()
        }

        return true
    }

    fun renameProfile(context: Context, oldName: String, newName: String): Boolean {
        if (newName.isBlank() || newName == DEFAULT_PROFILE_NAME) return false

        val prefs = context.getSharedPreferences(PROFILES_PREFS, MODE_PRIVATE)
        val profilesJson = prefs.getString(KEY_PROFILES, "[]")
        val type = object : TypeToken<List<String>>() {}.type
        val profiles: MutableList<String> = gson.fromJson(profilesJson, type) ?: mutableListOf()

        if (!profiles.contains(oldName)) return false
        if (profiles.contains(newName)) return false

        val wasDefault = (oldName == DEFAULT_PROFILE_NAME)

        if (wasDefault) {
            // For Default profile: rename it, then create a new Default with empty data
            profiles[profiles.indexOf(oldName)] = newName
            prefs.edit().putString(KEY_PROFILES, gson.toJson(profiles)).apply()

            // Rename SharedPreferences files
            renameSharedPreferences(context, "markers_data_$oldName", "markers_data_$newName")
            renameSharedPreferences(context, "app_settings_$oldName", "app_settings_$newName")
            renameSharedPreferences(context, "map_settings_$oldName", "map_settings_$newName")

            // Update active profile reference
            val activeProfile = prefs.getString(KEY_ACTIVE_PROFILE, DEFAULT_PROFILE_NAME)
            if (activeProfile == oldName) {
                prefs.edit().putString(KEY_ACTIVE_PROFILE, newName).apply()
            }

            // Create a new empty Default profile
            val newProfiles: MutableList<String> = mutableListOf(DEFAULT_PROFILE_NAME)
            newProfiles.addAll(profiles)
            prefs.edit().putString(KEY_PROFILES, gson.toJson(newProfiles)).apply()

            // Create empty SharedPreferences for new Default
            context.getSharedPreferences("markers_data_$DEFAULT_PROFILE_NAME", MODE_PRIVATE).edit().clear().apply()
            context.getSharedPreferences("app_settings_$DEFAULT_PROFILE_NAME", MODE_PRIVATE).edit().apply {
                putBoolean("music_enabled", true)
                putBoolean("snow_enabled", true)
            }.apply()
            context.getSharedPreferences("map_settings_$DEFAULT_PROFILE_NAME", MODE_PRIVATE).edit().clear().apply()

        } else {
            // For non-Default profiles: simple rename
            profiles[profiles.indexOf(oldName)] = newName
            prefs.edit().putString(KEY_PROFILES, gson.toJson(profiles)).apply()

            // Rename SharedPreferences files by copying data and deleting old files
            renameSharedPreferences(context, "markers_data_$oldName", "markers_data_$newName")
            renameSharedPreferences(context, "app_settings_$oldName", "app_settings_$newName")
            renameSharedPreferences(context, "map_settings_$oldName", "map_settings_$newName")

            // If the renamed profile was active, keep it active
            val activeProfile = prefs.getString(KEY_ACTIVE_PROFILE, DEFAULT_PROFILE_NAME)
            if (activeProfile == oldName) {
                prefs.edit().putString(KEY_ACTIVE_PROFILE, newName).apply()
            }
        }

        return true
    }

    private fun renameSharedPreferences(context: Context, oldName: String, newName: String) {
        val oldPrefs = context.getSharedPreferences(oldName, MODE_PRIVATE)
        val allEntries = oldPrefs.all

        val newPrefs = context.getSharedPreferences(newName, MODE_PRIVATE)
        val editor = newPrefs.edit()
        for ((key, value) in allEntries) {
            when (value) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Float -> editor.putFloat(key, value)
                is Long -> editor.putLong(key, value)
                else -> {}
            }
        }
        editor.apply()

        oldPrefs.edit().clear().apply()
    }

    fun exportProfileToJson(context: Context, profileName: String, uri: Uri): Result<String> {
        return try {
            val appCtx = context.applicationContext

            // Load markers
            val markerPrefs = appCtx.getSharedPreferences("markers_data_$profileName", MODE_PRIVATE)
            val markersJson = markerPrefs.getString("markers_json", "[]") ?: "[]"
            val gson = Gson()
            val markerType = object : TypeToken<List<Marker>>() {}.type
            val markers: List<Marker> = gson.fromJson(markersJson, markerType) ?: emptyList()

            // Load app settings
            val settingsPrefs = appCtx.getSharedPreferences("app_settings_$profileName", MODE_PRIVATE)
            val musicEnabled = settingsPrefs.getBoolean("music_enabled", true)
            val snowEnabled = settingsPrefs.getBoolean("snow_enabled", true)

            // Load map settings
            val mapPrefs = appCtx.getSharedPreferences("map_settings_$profileName", MODE_PRIVATE)
            val mapLat = mapPrefs.getFloat("lat", 0f)
            val mapLon = mapPrefs.getFloat("lon", 0f)
            val mapZoom = mapPrefs.getFloat("zoom", 0f)

            // Load custom marker types
            val customTypes = com.example.christmastrees.util.CustomMarkerManager.loadCustomTypes(appCtx, profileName)

            // Create export object
            val exportProfile = ExportProfile(
                version = EXPORT_VERSION,
                exportedProfile = profileName,
                markers = markers,
                customMarkerTypes = customTypes,
                musicEnabled = musicEnabled,
                snowEnabled = snowEnabled,
                mapLat = mapLat,
                mapLon = mapLon,
                mapZoom = mapZoom
            )

            // Serialize to JSON
            val exportJson = gson.toJson(exportProfile)

            // Write to file
            appCtx.contentResolver.openOutputStream(uri, "rwt")?.use { outputStream ->
                outputStream.write(exportJson.toByteArray(Charsets.UTF_8))
            }

            Log.d("ProfileManager", "Exported profile '$profileName' with ${markers.size} markers to $uri")
            Result.success("Профиль '$profileName' успешно экспортирован")
        } catch (e: Exception) {
            Log.e("ProfileManager", "Failed to export profile", e)
            Result.failure(e)
        }
    }

    fun importProfileFromJson(context: Context, uri: Uri, targetProfileName: String): Result<String> {
        return try {
            val appCtx = context.applicationContext

            // Read and parse JSON
            val json = appCtx.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return Result.failure(Exception("Не удалось прочитать файл"))

            val gson = Gson()
            val exportProfile = gson.fromJson(json, ExportProfile::class.java)

            // Validate version
            if (exportProfile.version != EXPORT_VERSION) {
                return Result.failure(Exception("Неподдерживаемая версия формата экспорта: ${exportProfile.version}"))
            }

            // Validate target profile exists
            val profileNames = getProfileNames(appCtx)
            if (!profileNames.contains(targetProfileName)) {
                return Result.failure(Exception("Профиль '$targetProfileName' не найден"))
            }

            // Save markers
            val markerPrefs = appCtx.getSharedPreferences("markers_data_$targetProfileName", MODE_PRIVATE)
            val markersJson = gson.toJson(exportProfile.markers)
            markerPrefs.edit().putString("markers_json", markersJson).commit()

            // Save custom marker types
            if (exportProfile.customMarkerTypes.isNotEmpty()) {
                com.example.christmastrees.util.CustomMarkerManager.saveCustomTypes(
                    appCtx,
                    targetProfileName,
                    exportProfile.customMarkerTypes
                )
            }

            // Save app settings
            val settingsPrefs = appCtx.getSharedPreferences("app_settings_$targetProfileName", MODE_PRIVATE)
            settingsPrefs.edit()
                .putBoolean("music_enabled", exportProfile.musicEnabled)
                .putBoolean("snow_enabled", exportProfile.snowEnabled)
                .commit()

            // Save map settings
            val mapPrefs = appCtx.getSharedPreferences("map_settings_$targetProfileName", MODE_PRIVATE)
            mapPrefs.edit()
                .putFloat("lat", exportProfile.mapLat)
                .putFloat("lon", exportProfile.mapLon)
                .putFloat("zoom", exportProfile.mapZoom)
                .commit()

            Log.d("ProfileManager", "Imported profile '$targetProfileName' with ${exportProfile.markers.size} markers from $uri")
            Result.success("Профиль '$targetProfileName' успешно импортирован (${exportProfile.markers.size} маркеров)")
        } catch (e: Exception) {
            Log.e("ProfileManager", "Failed to import profile", e)
            Result.failure(e)
        }
    }

    fun migrateDataToProfile(context: Context, profileName: String) {
        val markerPrefs = context.getSharedPreferences("markers_data", MODE_PRIVATE)
        val settingsPrefs = context.getSharedPreferences("app_settings", MODE_PRIVATE)
        val mapPrefs = context.getSharedPreferences("map_settings", MODE_PRIVATE)

        // Check if there's data to migrate
        if (markerPrefs.all.isEmpty() && settingsPrefs.all.isEmpty() && mapPrefs.all.isEmpty()) {
            return
        }

        // Migrate markers
        val markerProfilePrefs = context.getSharedPreferences("markers_data_$profileName", MODE_PRIVATE)
        val markerEditor = markerProfilePrefs.edit()
        for ((key, value) in markerPrefs.all) {
            when (value) {
                is String -> markerEditor.putString(key, value)
                is Int -> markerEditor.putInt(key, value)
                is Boolean -> markerEditor.putBoolean(key, value)
                is Float -> markerEditor.putFloat(key, value)
                is Long -> markerEditor.putLong(key, value)
                else -> {}
            }
        }
        markerEditor.apply()

        // Migrate app settings
        val settingsProfilePrefs = context.getSharedPreferences("app_settings_$profileName", MODE_PRIVATE)
        val settingsEditor = settingsProfilePrefs.edit()
        for ((key, value) in settingsPrefs.all) {
            when (value) {
                is String -> settingsEditor.putString(key, value)
                is Int -> settingsEditor.putInt(key, value)
                is Boolean -> settingsEditor.putBoolean(key, value)
                is Float -> settingsEditor.putFloat(key, value)
                is Long -> settingsEditor.putLong(key, value)
                else -> {}
            }
        }
        settingsEditor.apply()

        // Migrate map settings
        val mapProfilePrefs = context.getSharedPreferences("map_settings_$profileName", MODE_PRIVATE)
        val mapEditor = mapProfilePrefs.edit()
        for ((key, value) in mapPrefs.all) {
            when (value) {
                is String -> mapEditor.putString(key, value)
                is Int -> mapEditor.putInt(key, value)
                is Boolean -> mapEditor.putBoolean(key, value)
                is Float -> mapEditor.putFloat(key, value)
                is Long -> mapEditor.putLong(key, value)
                else -> {}
            }
        }
        mapEditor.apply()

        // Clear old default files
        markerPrefs.edit().clear().apply()
        settingsPrefs.edit().clear().apply()
        mapPrefs.edit().clear().apply()
    }
}
